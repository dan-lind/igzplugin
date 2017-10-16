package com.danlind.igz.misc;

import com.danlind.igz.Zorro;
import io.reactivex.Single;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Created by danlin on 2017-10-14.
 */
public class ExceptionHelper {

    public static String getErrorMessage(Throwable ex) {
        Zorro.indicateError();
        if (ex instanceof HttpClientErrorException) {
            HttpClientErrorException e = (HttpClientErrorException) ex;
            return "API call failed with status code " + e.getRawStatusCode() + " and body " + e.getResponseBodyAsString();
        } else {
            return "API call failed";
        }
    }
}
