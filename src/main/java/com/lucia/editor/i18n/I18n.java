/*
 * Copyright (c) 2026 Gabriel González
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.lucia.editor.i18n;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class I18n {

    private static final String BUNDLE_BASE = "i18n.messages";
    private static Locale currentLocale = Locale.ENGLISH;
    private static ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE, currentLocale);

    private I18n() {
    }

    public static synchronized void setLocale(Locale locale) {
        currentLocale = locale;
        bundle = ResourceBundle.getBundle(BUNDLE_BASE, currentLocale);
    }

    public static synchronized Locale getLocale() {
        return currentLocale;
    }

    public static synchronized String tr(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException ex) {
            return "!" + key + "!";
        }
    }
}
