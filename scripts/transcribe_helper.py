import sys
import os
import re
import subprocess
import imageio_ffmpeg
import speech_recognition as sr

# Patch SpeechRecognition AudioData.get_flac_data to use ffmpeg (completely avoids missing flac-linux-x86_64 on Alpine/musl)
_orig_get_flac_data = sr.AudioData.get_flac_data

def _safe_get_flac_data(self, convert_rate=None, convert_width=None):
    try:
        wav_data = self.get_wav_data(convert_rate, convert_width)
        ffmpeg_exe = imageio_ffmpeg.get_ffmpeg_exe()
        cmd = [ffmpeg_exe, "-y", "-f", "wav", "-i", "pipe:0", "-f", "flac", "pipe:1"]
        proc = subprocess.Popen(
            cmd,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL
        )
        flac_bytes, _ = proc.communicate(wav_data)
        if proc.returncode == 0 and flac_bytes:
            return flac_bytes
    except Exception:
        pass
    return _orig_get_flac_data(self, convert_rate, convert_width)

sr.AudioData.get_flac_data = _safe_get_flac_data


EXPENSE_CATEGORY_WORDS = (
    "zapravka|zaprafka|benzin|gaz|metan|propan|yoqilgi|yoqilg'i|"
    "taksi|taxi|yo'l|yol|avtobus|metro|"
    "obed|tushlik|ovqat|osh|somsa|kafe|restoran|lavash|burger|non|choy|kofe|shashlik|gosht|go'sht|"
    "bozor|magazin|supermarket|korzinka|havas|"
    "dori|dorixona|apteka|doktor|shifoxona|klinika|"
    "telefon|internet|paynet|svet|suv|arenda|kvartira|ijara|remont"
)

def normalize_uzbek_text(text: str) -> str:
    if not text:
        return ""
    t = text.strip()

    # 1. "ilmiy <expense_category>" -> "50 ming <expense_category>"
    # STT acoustic artifact: "elli min" / "ellik ming" heard as "ilmiy"
    t = re.sub(rf'\bilmiy\s+({EXPENSE_CATEGORY_WORDS})\b', r'50 ming \1', t, flags=re.IGNORECASE)

    # 2. "ilmiy ming" / "ilmiy min" -> "50 ming"
    t = re.sub(r'\bilmiy\s*(?:ming|min)\b', '50 ming', t, flags=re.IGNORECASE)

    # 3. "elli min" / "ellik min" / "ellimin" -> "50 ming"
    t = re.sub(r'\belli(?:k)?\s*min\b', '50 ming', t, flags=re.IGNORECASE)
    t = re.sub(r'\bellimin\b', '50 ming', t, flags=re.IGNORECASE)

    # 4. "<digits> min" -> "<digits> ming" (e.g. "50 min zaprafka" -> "50 ming zapravka")
    t = re.sub(r'\b(\d+)\s*min(?:i|ga|dan)?\b', r'\1 ming', t, flags=re.IGNORECASE)

    # 5. "zaprafka" -> "zapravka"
    t = re.sub(r'\bzaprafka\b', 'zapravka', t, flags=re.IGNORECASE)
    t = re.sub(r'\bzaprafkaga\b', 'zapravkaga', t, flags=re.IGNORECASE)

    return t

def transcribe(audio_path):
    if not os.path.exists(audio_path):
        print("ERROR: File not found", file=sys.stderr)
        sys.exit(1)

    ffmpeg_exe = imageio_ffmpeg.get_ffmpeg_exe()
    wav_path = audio_path + ".wav"

    try:
        # Convert OGG / any audio to 16kHz mono WAV with audio volume boost & highpass filter
        cmd = [
            ffmpeg_exe, "-y", "-i", audio_path,
            "-af", "volume=1.8,highpass=f=100,lowpass=f=7500",
            "-ar", "16000", "-ac", "1", wav_path
        ]
        subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)

        recognizer = sr.Recognizer()
        with sr.AudioFile(wav_path) as source:
            audio_data = recognizer.record(source)

        # Transcribe with Google Web Speech API in Uzbek (with multi-hypothesis inspection)
        try:
            raw_res = recognizer.recognize_google(audio_data, language="uz-UZ", show_all=True)
            chosen_text = None

            if isinstance(raw_res, dict) and "alternative" in raw_res and raw_res["alternative"]:
                alternatives = [alt.get("transcript", "").strip() for alt in raw_res["alternative"] if alt.get("transcript")]
                if alternatives:
                    # Preference 1: Candidate that contains digits or financial keywords
                    financial_keywords = ["ming", "min", "so'm", "som", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "ellik", "o'ttiz", "yigirma", "o'n", "yuz", "million", "zapravka", "zaprafka"]
                    for cand in alternatives:
                        if any(kw in cand.lower() for kw in financial_keywords):
                            chosen_text = cand
                            break
                    if not chosen_text:
                        chosen_text = alternatives[0]

            if not chosen_text:
                chosen_text = recognizer.recognize_google(audio_data, language="uz-UZ")

            final_text = normalize_uzbek_text(chosen_text)
            print(final_text)

        except sr.UnknownValueError:
            print("ERROR: Ovozdan so'zlar aniqlanmadi", file=sys.stderr)
            sys.exit(2)
        except sr.RequestError as e:
            print(f"ERROR: Google API request error: {e}", file=sys.stderr)
            sys.exit(3)

    except Exception as e:
        print(f"ERROR: {e}", file=sys.stderr)
        sys.exit(4)
    finally:
        if os.path.exists(wav_path):
            try:
                os.remove(wav_path)
            except Exception:
                pass

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python transcribe_helper.py <audio_file>", file=sys.stderr)
        sys.exit(1)
    transcribe(sys.argv[1])
