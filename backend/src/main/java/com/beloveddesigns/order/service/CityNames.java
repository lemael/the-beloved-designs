package com.beloveddesigns.order.service;

import java.text.Normalizer;
import java.util.Locale;

/** Comparaison de noms de villes saisis à la main : « Yaoundé », « yaounde », «  YAOUNDÉ  » → « yaounde ». */
public final class CityNames {

    private CityNames() {
    }

    public static String normalize(String city) {
        if (city == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(city, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
