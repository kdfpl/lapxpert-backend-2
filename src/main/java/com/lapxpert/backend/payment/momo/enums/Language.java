package com.lapxpert.backend.payment.momo.enums;
import com.google.gson.annotations.SerializedName;
import com.lapxpert.backend.payment.momo.shared.constants.Constants;

public enum Language {

    @SerializedName("vi")
    VI(Constants.LANGUAGE_VI),

    @SerializedName("en")
    EN(Constants.LANGUAGE_EN);

    private final String value;

    Language(String value) {
        this.value = value;
    }

    public static Language findByName(String name) {
        for (Language type : values()) {
            if (type.getLanguage().equals(name)) {
                return type;
            }
        }
        return null;
    }

    public String getLanguage() { return value; }

}
