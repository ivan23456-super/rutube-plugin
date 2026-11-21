package org.pipepipe.rutube.linkHandler;

import org.pipepipe.rutube.RuTubeParsingHelper;
import org.pipepipe.rutube.search.filter.RuTubeFilters;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.search.filter.Filter;
import org.schabi.newpipe.extractor.search.filter.FilterItem;

import java.net.URL;
import java.util.List;

/**
 * Builds the RuTube search API URLs. The content filter selects the endpoint:
 * videos/playlists use the pangolin backend, channels use the combined cards
 * endpoint and Shorts use the shorts endpoint.
 */
public final class RuTubeSearchQueryHandlerFactory extends SearchQueryHandlerFactory {

    private static final RuTubeSearchQueryHandlerFactory INSTANCE =
            new RuTubeSearchQueryHandlerFactory();

    private final RuTubeFilters searchFilters = new RuTubeFilters();

    private RuTubeSearchQueryHandlerFactory() {
    }

    public static RuTubeSearchQueryHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getUrl(final String searchString,
                         final List<FilterItem> selectedContentFilter,
                         final List<FilterItem> selectedSortFilter)
            throws ParsingException {
        return getUrl(searchString, selectedContentFilter, selectedSortFilter,
                RuTubeParsingHelper.BASE_URL);
    }

    @Override
    public String getUrl(final String searchString,
                         final List<FilterItem> selectedContentFilter,
                         final List<FilterItem> selectedSortFilter,
                         final String baseUrl) throws ParsingException {
        searchFilters.setSelectedSortFilter(selectedSortFilter);
        searchFilters.setSelectedContentFilter(selectedContentFilter);
        final String filterQuery = searchFilters.evaluateSelectedFilters(searchString);

        final String contentType = getContentType(selectedContentFilter);
        final String encodedQuery = RuTubeParsingHelper.encode(searchString);

        switch (contentType) {
            case RuTubeFilters.CONTENT_CHANNELS:
                return RuTubeParsingHelper.SEARCH_CHANNELS_API
                        + "?query=" + encodedQuery
                        + "&page=1&fields=video_count,is_official&content_type=channel"
                        + filterQuery;
            case RuTubeFilters.CONTENT_SHORTS:
                return RuTubeParsingHelper.SEARCH_SHORTS_API
                        + "?query=" + encodedQuery
                        + "&page=1&per_page=" + RuTubeParsingHelper.ITEMS_PER_PAGE
                        + filterQuery;
            case RuTubeFilters.CONTENT_VIDEOS:
            case RuTubeFilters.CONTENT_PLAYLISTS:
            default:
                return RuTubeParsingHelper.SEARCH_VIDEOS_API
                        + "?query=" + encodedQuery
                        + "&page=1"
                        + filterQuery;
        }
    }

    @Override
    public String getSearchString(final String url) {
        try {
            final URL urlObj = new URL(url);
            final String query = urlObj.getQuery();
            if (query == null) {
                return "";
            }
            for (final String pair : query.split("&")) {
                final int eq = pair.indexOf('=');
                if (eq > 0 && pair.substring(0, eq).equals("query")) {
                    return java.net.URLDecoder.decode(pair.substring(eq + 1),
                            org.schabi.newpipe.extractor.utils.Utils.UTF_8);
                }
            }
        } catch (final Exception ignored) {
            // fall through
        }
        return "";
    }

    private static String getContentType(final List<FilterItem> selectedContentFilter) {
        if (selectedContentFilter == null || selectedContentFilter.isEmpty()) {
            return RuTubeFilters.CONTENT_VIDEOS;
        }
        return selectedContentFilter.get(0).getName();
    }

    @Override
    public Filter getAvailableContentFilter() {
        return searchFilters.getContentFilters();
    }

    @Override
    public Filter getAvailableSortFilter() {
        return searchFilters.getSortFilters();
    }

    @Override
    public Filter getContentFilterSortFilterVariant(final int contentFilterId) {
        return searchFilters.getContentFilterSortFilterVariant(contentFilterId);
    }

    @Override
    public FilterItem getFilterItem(final int filterId) {
        return searchFilters.getFilterItem(filterId);
    }
}
