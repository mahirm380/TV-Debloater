package tv.dumbtv.home;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Current weather from Open-Meteo, with a small local cache for offline starts. */
final class Weather {
    static final long REFRESH_MS = 30 * 60 * 1000L;
    private static final String ENDPOINT = "https://api.open-meteo.com/v1/forecast";
    private static final Pattern TEMPERATURE = Pattern.compile(
            "\\\"temperature_2m\\\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)");
    private static final Pattern APPARENT = Pattern.compile(
            "\\\"apparent_temperature\\\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)");
    private static final Pattern CODE = Pattern.compile("\\\"weather_code\\\"\\s*:\\s*([0-9]+)");

    static final class Reading {
        final float temperature;
        final float apparent;
        final int code;
        final long time;

        Reading(float temperature, float apparent, int code, long time) {
            this.temperature = temperature;
            this.apparent = apparent;
            this.code = code;
            this.time = time;
        }

        String condition(Context context) {
            if (code == 0) return context.getString(R.string.weather_clear);
            if (code <= 2) return context.getString(R.string.weather_partly_cloudy);
            if (code == 3) return context.getString(R.string.weather_cloudy);
            if (code == 45 || code == 48) return context.getString(R.string.weather_fog);
            if (code >= 51 && code <= 57) return context.getString(R.string.weather_drizzle);
            if (code >= 61 && code <= 67) return context.getString(R.string.weather_rain);
            if (code >= 71 && code <= 77) return context.getString(R.string.weather_snow);
            if (code >= 80 && code <= 82) return context.getString(R.string.weather_rain_showers);
            if (code == 85 || code == 86) return context.getString(R.string.weather_snow_showers);
            if (code >= 95) return context.getString(R.string.weather_thunderstorm);
            return context.getString(R.string.weather_current);
        }
    }

    private Weather() {}

    static Reading fetch(Config config) throws IOException {
        String latitude = coordinate(config, "weather.latitude", "51.5074", -90, 90);
        String longitude = coordinate(config, "weather.longitude", "-0.1278", -180, 180);
        String unit = config.get("weather.unit", "celsius");
        if (!"fahrenheit".equals(unit)) unit = "celsius";
        String timezone = URLEncoder.encode(config.get("weather.timezone", "auto"), "UTF-8");
        String endpoint = ENDPOINT + "?latitude=" + latitude + "&longitude=" + longitude
                + "&current=temperature_2m,apparent_temperature,weather_code"
                + "&temperature_unit=" + unit + "&timezone=" + timezone;
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setConnectTimeout(6000);
        connection.setReadTimeout(6000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "dumbtv-home-weather/1.0");
        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) throw new IOException("weather HTTP " + status);
            String json = read(connection.getInputStream());
            return new Reading(number(TEMPERATURE, json), number(APPARENT, json),
                    Math.round(number(CODE, json)), System.currentTimeMillis());
        } finally {
            connection.disconnect();
        }
    }

    static Reading cached(Context context, Config config) {
        SharedPreferences p = context.getSharedPreferences("weather", Context.MODE_PRIVATE);
        if (!cacheKey(config).equals(p.getString("location", ""))) return null;
        long time = p.getLong("time", 0);
        if (time == 0) return null;
        return new Reading(p.getFloat("temperature", 0), p.getFloat("apparent", 0),
                p.getInt("code", 0), time);
    }

    static void save(Context context, Config config, Reading reading) {
        context.getSharedPreferences("weather", Context.MODE_PRIVATE).edit()
                .putString("location", cacheKey(config))
                .putFloat("temperature", reading.temperature)
                .putFloat("apparent", reading.apparent)
                .putInt("code", reading.code)
                .putLong("time", reading.time)
                .apply();
    }

    static String place(Config config) {
        return config.get("weather.name", "London");
    }

    static String unitSymbol(Config config) {
        return "fahrenheit".equals(config.get("weather.unit", "celsius")) ? "°F" : "°";
    }

    private static String cacheKey(Config config) {
        return config.get("weather.latitude", "51.5074") + ","
                + config.get("weather.longitude", "-0.1278") + ","
                + config.get("weather.unit", "celsius");
    }

    private static String coordinate(Config config, String key, String fallback,
            double minimum, double maximum) throws IOException {
        String value = config.get(key, fallback);
        try {
            double number = Double.parseDouble(value);
            if (number < minimum || number > maximum) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            throw new IOException("invalid " + key, e);
        }
    }

    private static String read(InputStream stream) throws IOException {
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null; ) out.append(line);
        }
        return out.toString();
    }

    private static float number(Pattern pattern, String json) throws IOException {
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) throw new IOException("weather response missing field");
        try {
            return Float.parseFloat(matcher.group(1));
        } catch (NumberFormatException e) {
            throw new IOException("invalid weather value", e);
        }
    }
}
