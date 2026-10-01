package com.hisobchi.bot.notification.service;

import com.hisobchi.bot.notification.entity.NotificationSettings;
import com.hisobchi.bot.notification.repository.NotificationSettingsRepository;
import com.hisobchi.bot.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSettingsService {

    private final NotificationSettingsRepository settingsRepository;

    @Transactional
    public NotificationSettings getOrCreateSettings(User user) {
        return settingsRepository.findByUserId(user.getId())
                .orElseGet(() -> settingsRepository.save(
                        NotificationSettings.builder()
                                .user(user)
                                .profitReminder18Enabled(true)
                                .profitReminder21Enabled(true)
                                .dailyReportEnabled(true)
                                .weeklyReportEnabled(true)
                                .twoWeekReportEnabled(true)
                                .threeWeekReportEnabled(true)
                                .monthlyReportEnabled(true)
                                .dailyReportTime("23:00")
                                .build()
                ));
    }

    @Transactional
    public NotificationSettings toggleSetting(User user, String settingKey) {
        NotificationSettings settings = getOrCreateSettings(user);
        switch (settingKey) {
            case "rem_18" -> settings.setProfitReminder18Enabled(!settings.getProfitReminder18Enabled());
            case "rem_21" -> settings.setProfitReminder21Enabled(!settings.getProfitReminder21Enabled());
            case "rep_daily" -> settings.setDailyReportEnabled(!settings.getDailyReportEnabled());
            case "rep_weekly" -> settings.setWeeklyReportEnabled(!settings.getWeeklyReportEnabled());
            case "rep_2week" -> settings.setTwoWeekReportEnabled(!settings.getTwoWeekReportEnabled());
            case "rep_3week" -> settings.setThreeWeekReportEnabled(!settings.getThreeWeekReportEnabled());
            case "rep_monthly" -> settings.setMonthlyReportEnabled(!settings.getMonthlyReportEnabled());
            default -> log.warn("Unknown notification setting toggle: {}", settingKey);
        }
        return settingsRepository.save(settings);
    }
}
