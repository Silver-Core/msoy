//
// $Id$

package com.threerings.msoy.web.gwt;

import java.util.Arrays;
import java.util.List;

/**
 * A place where we can encapsulate the creation of arguments that link to complex pages in Whirled
 * that are required on the server and on the client. We do this here so that we don't have
 * references between otherwise unrelated classes and services introduced by the fact that we want
 * a Hyperlink from somewhere in Whirled to somewhere (from a code standpoint) totally unrelated.
 * See also NaviUtil in src/gwt.
 */
public class SharedNaviUtil
{
    /**
     * Creates a request string for the given url and parameters. Does not do URI escaping.
     * @param nameValuePairs pairs to append; even number of elements expected
     */
    public static String buildRequest (String url, String... nameValuePairs)
    {
        return buildRequest(url, Arrays.asList(nameValuePairs));
    }

    /**
     * Creates a request string for the given url and parameters. Does not do URI escaping.
     * @param nameValuePairs pairs to append; even number of elements expected
     */
    public static String buildRequest (String url, List<String> nameValuePairs)
    {
        int size = nameValuePairs.size();
        if (size % 2 != 0) {
            throw new IllegalArgumentException("Expected even number of arguments, got " + size);
        }
        if (size > 0) {
            int join = url.contains("?") ? 0 : 2;
            for (String str : nameValuePairs) {
                url += JOINERS[join] + str;
                join = (join + 1) & 1;
            }
        }
        return url;
    }

    public enum GameDetails {
        INSTRUCTIONS("i"), COMMENTS("c"), TROPHIES("t"), MYRANKINGS("mr"),
        TOPRANKINGS("tr"), METRICS("m"), LOGS("l"), DEV_LOGS("d");

        public String code () {
            return _code;
        }

        public Args args (int gameId) {
            return Args.compose("d", gameId, code());
        }

        GameDetails (String code) {
            _code = code;
        }

        /**
         * Look up a GameDetails by its code.
         */
        public static GameDetails getByCode (String code)
        {
            // we could store these in a map, but why bother?
            for (GameDetails detail : values()) {
                if (detail.code().equals(code)) {
                    return detail;
                }
            }
            return GameDetails.INSTRUCTIONS;
        }

        protected String _code;
    }

    protected static final String[] JOINERS = {"&", "=", "?"};
}
