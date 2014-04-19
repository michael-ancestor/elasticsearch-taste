package org.codelibs.elasticsearch.taste.similarity.worker;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.recommender.ItemBasedRecommender;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.similarity.precompute.SimilarItems;
import org.apache.mahout.cf.taste.similarity.precompute.SimilarItemsWriter;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

public class SimilarItemsWorker implements Runnable {
    private static final ESLogger logger = Loggers
            .getLogger(SimilarItemsWorker.class);

    int number;

    ItemBasedRecommender recommender;

    LongPrimitiveIterator itemIDs;

    int numOfMostSimilarItems;

    SimilarItemsWriter writer;

    public SimilarItemsWorker(final int number,
            final ItemBasedRecommender recommender,
            final LongPrimitiveIterator itemIDs,
            final int numOfMostSimilarItems, final SimilarItemsWriter writer) {
        this.number = number;
        this.recommender = recommender;
        this.itemIDs = itemIDs;
        this.numOfMostSimilarItems = numOfMostSimilarItems;
        this.writer = writer;
    }

    @Override
    public void run() {
        logger.info("Worker {} is started.", number);
        long itemID;
        while ((itemID = nextId(itemIDs)) != -1) {
            try {
                final List<RecommendedItem> recommendedItems = recommender
                        .mostSimilarItems(itemID, numOfMostSimilarItems);
                final SimilarItems similarItems = new SimilarItems(itemID,
                        recommendedItems);
                writer.add(similarItems);
            } catch (TasteException | IOException e) {
                logger.error("Item {} could not be processed.", e, itemID);
            }
        }
        logger.info("Worker {} is complated.", number);
    }

    private long nextId(final LongPrimitiveIterator itemIDs) {
        synchronized (itemIDs) {
            try {
                return itemIDs.nextLong();
            } catch (final NoSuchElementException e) {
                return -1;
            }
        }
    }
}
