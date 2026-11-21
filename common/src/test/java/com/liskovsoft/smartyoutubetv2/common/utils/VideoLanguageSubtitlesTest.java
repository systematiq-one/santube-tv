package com.liskovsoft.smartyoutubetv2.common.utils;

import com.liskovsoft.mediaserviceinterfaces.data.MediaFormat;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo;
import com.liskovsoft.mediaserviceinterfaces.data.MediaSubtitle;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class VideoLanguageSubtitlesTest {
    private static final String CAPTIONS_URL = "https://example.com/api/timedtext?v=abc";

    @Test
    public void picksTheTranscriptOfTheOriginalAudio() {
        MediaSubtitle spanish = asr("es", "Spanish (auto-generated)");
        MediaItemFormatInfo info = formatInfo(
                audio("es-US.4"),
                Arrays.asList(asr("en", "English (auto-generated)"), spanish));

        assertSame(spanish, VideoLanguageSubtitles.findOrigin(info));
    }

    @Test
    public void skipsTheTranscriptsOfAutoDubs() {
        // An English video dubbed into Arabic: the Arabic transcript is listed first
        MediaSubtitle english = asr("en", "English (auto-generated)");
        MediaItemFormatInfo info = formatInfo(
                audio("ar.3", "en-US.4"),
                Arrays.asList(dubTranscript("ar", "Arabic (auto-generated)"), english));

        assertSame(english, VideoLanguageSubtitles.findOrigin(info));
    }

    @Test
    public void prefersTheRealTranscriptOverTheDubOneInTheSameLanguage() {
        MediaSubtitle real = asr("en", "English (auto-generated)");
        MediaItemFormatInfo info = formatInfo(
                audio("en.4"),
                Arrays.asList(dubTranscript("en", "English (auto-generated)"), real));

        assertSame(real, VideoLanguageSubtitles.findOrigin(info));
    }

    @Test
    public void withoutAnOriginalAudioTrackTakesTheTranscriptThatIsNotOfADub() {
        MediaSubtitle french = asr("fr", "French (auto-generated)");
        MediaItemFormatInfo info = formatInfo(
                audio("de.3"),
                Arrays.asList(dubTranscript("de", "German (auto-generated)"), french));

        assertSame(french, VideoLanguageSubtitles.findOrigin(info));
    }

    @Test
    public void fallsBackToEnglishThenToAnyTranscript() {
        MediaSubtitle english = asr("en", "English (auto-generated)");
        MediaSubtitle german = asr("de", "German (auto-generated)");

        // The video's language (ja) has no transcript
        assertSame(english, VideoLanguageSubtitles.findOrigin(
                formatInfo(audio("ja.4"), Arrays.asList(german, english))));
        assertSame(german, VideoLanguageSubtitles.findOrigin(
                formatInfo(audio("ja.4"), Arrays.asList(german))));
    }

    @Test
    public void withoutTranscriptsTakesTheVideoOwnCaptionsNotATranslation() {
        MediaSubtitle translation = subtitle("fr", "French", null, CAPTIONS_URL + "&lang=en&tlang=fr");
        MediaSubtitle own = subtitle("en", "English", null, CAPTIONS_URL + "&lang=en");
        MediaItemFormatInfo info = formatInfo(audio("en.4"), Arrays.asList(translation, own));

        assertSame(own, VideoLanguageSubtitles.findOrigin(info));
    }

    @Test
    public void nothingToPickWithoutCaptions() {
        assertNull(VideoLanguageSubtitles.findOrigin(null));
        assertNull(VideoLanguageSubtitles.findOrigin(formatInfo(audio("en.4"), null)));
        assertNull(VideoLanguageSubtitles.findOrigin(formatInfo(audio("en.4"), new ArrayList<>())));
    }

    @Test
    public void trackLanguageIsTheNameAsThePlayerStoresIt() {
        // The player's track list (FormatItem) needs Android to load, so the match itself
        // isn't covered here: only the key it is matched by
        assertEquals("spanish (auto-generated)", VideoLanguageSubtitles.getTrackLanguage(asr("es", "Spanish (auto-generated)")));
        assertEquals("es", VideoLanguageSubtitles.getTrackLanguage(asr("es", null)));
        assertNull(VideoLanguageSubtitles.getTrackLanguage(null));
        assertNull(VideoLanguageSubtitles.findFormat(null, null));
    }

    @Test
    public void baseLanguageDropsRegionAndTrackKind() {
        assertEquals("en", VideoLanguageSubtitles.baseLanguage("en-US.4"));
        assertEquals("pt", VideoLanguageSubtitles.baseLanguage("pt_BR"));
        assertEquals("es", VideoLanguageSubtitles.baseLanguage("ES"));
        assertNull(VideoLanguageSubtitles.baseLanguage(null));
    }

    private static MediaSubtitle asr(String languageCode, String name) {
        return subtitle(languageCode, name, "asr", CAPTIONS_URL + "&kind=asr&lang=" + languageCode);
    }

    private static MediaSubtitle dubTranscript(String languageCode, String name) {
        return subtitle(languageCode, name, "asr", CAPTIONS_URL + "&kind=asr&lang=" + languageCode + "&variant=timing-optimized");
    }

    private static MediaSubtitle subtitle(String languageCode, String name, String type, String baseUrl) {
        Map<String, Object> values = new HashMap<>();
        values.put("getLanguageCode", languageCode);
        values.put("getName", name);
        values.put("getType", type);
        values.put("getBaseUrl", baseUrl);

        return stub(MediaSubtitle.class, values);
    }

    private static List<MediaFormat> audio(String... audioTrackIds) {
        List<MediaFormat> result = new ArrayList<>();

        // A video format first: it has no audio track id
        result.add(stub(MediaFormat.class, new HashMap<>()));

        for (String audioTrackId : audioTrackIds) {
            Map<String, Object> values = new HashMap<>();
            values.put("getAudioTrackId", audioTrackId);
            result.add(stub(MediaFormat.class, values));
        }

        return result;
    }

    private static MediaItemFormatInfo formatInfo(List<MediaFormat> adaptiveFormats, List<MediaSubtitle> subtitles) {
        Map<String, Object> values = new HashMap<>();
        values.put("getAdaptiveFormats", adaptiveFormats);
        values.put("getSubtitles", subtitles);

        return stub(MediaItemFormatInfo.class, values);
    }

    /**
     * An instance that answers the listed getters and nothing else (no mocking library in this module)
     */
    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Map<String, Object> values) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return type.getSimpleName() + values;
            }

            Object value = values.get(method.getName());

            if (value == null && method.getReturnType() == boolean.class) {
                return false;
            }

            if (value == null && method.getReturnType().isPrimitive()) {
                return 0;
            }

            return value;
        });
    }
}
