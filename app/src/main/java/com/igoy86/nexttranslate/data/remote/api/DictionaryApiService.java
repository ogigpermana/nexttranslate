package com.igoy86.nexttranslate.data.remote.api;

import com.igoy86.nexttranslate.data.remote.dto.DictionaryResponseDto;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit service interface for the Free Dictionary API.
 *
 * <p>This interface belongs to the data layer. Retrofit generates its
 * implementation at runtime via {@link retrofit2.Retrofit#create(Class)}.</p>
 *
 * <p>Base URL: {@code https://freedictionaryapi.com/api/v1/}</p>
 *
 * <p>No API key is required — the Free Dictionary API is completely free
 * and publicly accessible without authentication.</p>
 *
 * <p>Instantiated by {@link com.igoy86.nexttranslate.di.AppContainer}.</p>
 */
public interface DictionaryApiService {

    /**
     * Retrieves dictionary entries for a given word in a specific language.
     *
     * <p>HTTP: {@code GET /entries/{language}/{word}}</p>
     *
     * <p>On success, the response body contains a {@link DictionaryResponseDto}
     * with a populated {@code entries} list. If the word is not found, the API
     * returns a 404 response with an error body.</p>
     *
     * @param language ISO 639-1/639-3 language code (e.g. "en", "id"),
     *                 or {@code "all"} to retrieve entries across all languages;
     *                 must not be null
     * @param word     the word to look up; must not be null or empty
     * @return a Retrofit {@link Call} wrapping the {@link DictionaryResponseDto}
     */
    @GET("entries/{language}/{word}")
    Call<DictionaryResponseDto> getEntries(
            @Path("language") String language,
            @Path("word") String word
    );

    /**
     * Retrieves dictionary entries for a given word, optionally including
     * cross-language translations for each sense.
     *
     * <p>HTTP: {@code GET /entries/{language}/{word}?translations=true}</p>
     *
     * <p>When {@code translations} is {@code true}, each
     * {@link com.igoy86.nexttranslate.data.remote.dto.DictionaryResponseDto.SenseDto}
     * in the response will include a {@code translations} list mapping the
     * sense to equivalent words in other available languages.</p>
     *
     * @param language     ISO 639-1/639-3 language code; must not be null
     * @param word         the word to look up; must not be null or empty
     * @param translations {@code true} to include per-sense translations;
     *                     {@code false} or {@code null} to omit them
     * @return a Retrofit {@link Call} wrapping the {@link DictionaryResponseDto}
     */
    @GET("entries/{language}/{word}")
    Call<DictionaryResponseDto> getEntriesWithTranslations(
            @Path("language") String language,
            @Path("word") String word,
            @Query("translations") boolean translations
    );
}
