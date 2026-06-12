package com.igoy86.nexttranslate.data.remote.api;

import com.igoy86.nexttranslate.data.remote.dto.TranslateRequest;
import com.igoy86.nexttranslate.data.remote.dto.TranslateResponse;
import com.igoy86.nexttranslate.data.remote.dto.ExplainRequest;
import com.igoy86.nexttranslate.data.remote.dto.ExplainResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Retrofit service interface defining the NextTranslate backend API contract.
 *
 * <p>This interface belongs to the data layer. Retrofit generates its
 * implementation at runtime via {@link retrofit2.Retrofit#create(Class)}.</p>
 *
 * <p>All requests require a Bearer token passed through the
 * {@code Authorization} header. The token must match the {@code APP_TOKEN}
 * environment variable configured on the Vercel backend.</p>
 *
 * <p>Base URL is configured in {@link com.igoy86.nexttranslate.di.AppContainer}
 * and should point to your deployed Vercel endpoint, for example:
 * {@code https://your-app.vercel.app/}</p>
 */
public interface TranslateApiService {

    /**
     * Sends a translation request to the backend and returns the translated text.
     *
     * <p>HTTP: {@code POST /api/translate}</p>
     *
     * <p>On success the response body contains a {@link TranslateResponse}
     * with {@code success = true} and a populated {@code data} object.
     * On failure the response body contains {@code success = false} and
     * an {@code error} string describing the problem.</p>
     *
     * @param authHeader the full Authorization header value in the form
     *                   {@code "Bearer <token>"}; must not be null
     * @param request    the {@link TranslateRequest} body containing the
     *                   text and language pair; must not be null
     * @return a Retrofit {@link Call} wrapping the {@link TranslateResponse}
     */
    @POST("api/translate")
    Call<TranslateResponse> translate(
            @Header("Authorization") String authHeader,
            @Body TranslateRequest request
    );
	
	/**
     * Sends a word-explanation request to the backend and returns an AI explanation.
     *
     * <p>HTTP: {@code POST /api/explain}</p>
     *
     * @param authHeader the full Authorization header value in the form
     *                   {@code "Bearer <token>"}; must not be null
     * @param request    the {@link ExplainRequest} body; must not be null
     * @return a Retrofit {@link Call} wrapping the {@link ExplainResponse}
     */
    @POST("api/explain")
    Call<ExplainResponse> explain(
            @Header("Authorization") String authHeader,
            @Body ExplainRequest request
    );
}
