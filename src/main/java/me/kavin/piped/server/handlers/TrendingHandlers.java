package me.kavin.piped.server.handlers;

import me.kavin.piped.utils.ExceptionHandler;
import me.kavin.piped.utils.obj.ContentItem;
import me.kavin.piped.utils.resp.InvalidRequestResponse;
import org.schabi.newpipe.extractor.Extractor;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.kiosk.KioskExtractor;
import org.schabi.newpipe.extractor.kiosk.KioskInfo;
import org.schabi.newpipe.extractor.kiosk.KioskList;
import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeTrendingGamingVideosLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeTrendingMoviesAndShowsTrailersLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeTrendingMusicLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeTrendingPodcastsEpisodesLinkHandlerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static me.kavin.piped.consts.Constants.YOUTUBE_SERVICE;
import static me.kavin.piped.consts.Constants.mapper;
import static me.kavin.piped.utils.CollectionUtils.collectRelatedItems;

public class TrendingHandlers {
    public static byte[] trendingResponse(String region)
            throws ExtractionException, IOException {

        if (region == null)
            ExceptionHandler.throwErrorResponse(new InvalidRequestResponse("region is a required parameter"));

        KioskList kioskList = YOUTUBE_SERVICE.getKioskList();
        kioskList.forceContentCountry(new ContentCountry(region));
        List<String> kiosks = new ArrayList<>();
        kiosks.add(YoutubeTrendingMusicLinkHandlerFactory.KIOSK_ID);
        kiosks.add(YoutubeTrendingGamingVideosLinkHandlerFactory.KIOSK_ID);
        kiosks.add(YoutubeTrendingMoviesAndShowsTrailersLinkHandlerFactory.KIOSK_ID);
        List<KioskExtractor<?>> extractors = new ArrayList<>();
        for (String k : kiosks) {
            extractors.add(kioskList.getExtractorById(k, null));
        }
        List<ContentItem> relatedStreams = new ArrayList<>();
        for (KioskExtractor<?> extractor : extractors) {
            extractor.fetchPage();
            KioskInfo info = KioskInfo.getInfo(extractor);
            List<ContentItem> items = new ArrayList<>(collectRelatedItems(info.getRelatedItems()));
            System.out.printf("%s: %d\n", extractor.getId(), items.size());
            for (int i = 0; i < items.size(); i++) {
                if (i > 5) break;
                ContentItem debugItem = items.get(i);
                System.out.println(debugItem.url);
            }
            Collections.shuffle(items);
            relatedStreams.addAll(items.subList(0, Math.min(10, items.size())));
        }

        // all kiosks are merged in relatedStreams, shuffle it, strip it to first 20, then return it
        Collections.shuffle(relatedStreams);
        if (relatedStreams.size() > 20) {
            relatedStreams.subList(20, relatedStreams.size()).clear();
        }

        System.out.println("OUTPUT:");
        for (int i = 0; i < relatedStreams.size(); i++) {
            if (i > 5) break;
            ContentItem debugItem = relatedStreams.get(i);
            System.out.println(debugItem.url);
        }

        return mapper.writeValueAsBytes(relatedStreams);
    }
}
