package com.campusbite.canteen.dto;

import com.campusbite.canteen.CanteenSettings;

public record CanteenSettingsDto(String canteenName, String openingTime, String closingTime, boolean orderingEnabled) {
    public static CanteenSettingsDto from(CanteenSettings s) {
        return new CanteenSettingsDto(s.getCanteenName(), s.getOpeningTime().toString(), s.getClosingTime().toString(), s.isOrderingEnabled());
    }
}
