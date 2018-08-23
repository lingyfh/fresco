/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.QualityInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.systrace.FrescoSystrace;

/**
 * Memory cache producer for the bitmap memory cache.
 */
public class BitmapMemoryCacheProducer implements Producer<CloseableReference<CloseableImage>> {

  public static final String PRODUCER_NAME = "BitmapMemoryCacheProducer";
  public static final String EXTRA_CACHED_VALUE_FOUND = ProducerConstants.EXTRA_CACHED_VALUE_FOUND;

  private final MemoryCache<CacheKey, CloseableImage> mMemoryCache;
  private final CacheKeyFactory mCacheKeyFactory;
  private final Producer<CloseableReference<CloseableImage>> mInputProducer;

  public BitmapMemoryCacheProducer(
      MemoryCache<CacheKey, CloseableImage> memoryCache,
      CacheKeyFactory cacheKeyFactory,
      Producer<CloseableReference<CloseableImage>> inputProducer) {
    mMemoryCache = memoryCache;
    mCacheKeyFactory = cacheKeyFactory;
    mInputProducer = inputProducer;
  }

  @Override
  public void produceResults(
      final Consumer<CloseableReference<CloseableImage>> consumer,
      final ProducerContext producerContext) {
    try {
      FrescoSystrace.beginSection("BitmapMemoryCacheProducer#produceResults");
      final ProducerListener listener = producerContext.getListener();
      final String requestId = producerContext.getId();
      listener.onProducerStart(requestId, getProducerName());
      final ImageRequest imageRequest = producerContext.getImageRequest();
      final Object callerContext = producerContext.getCallerContext();
      final CacheKey cacheKey = mCacheKeyFactory.getBitmapCacheKey(imageRequest, callerContext);

      CloseableReference<CloseableImage> cachedReference = mMemoryCache.get(cacheKey);

      if (cachedReference != null) {
        boolean isFinal = cachedReference.get().getQualityInfo().isOfFullQuality();
        if (isFinal) {
          listener.onProducerFinishWithSuccess(
              requestId,
              getProducerName(),
              listener.requiresExtraMap(requestId)
                  ? ImmutableMap.of(EXTRA_CACHED_VALUE_FOUND, "true")
                  : null);
          listener.onUltimateProducerReached(requestId, getProducerName(), true);
          consumer.onProgressUpdate(1f);
        }
        consumer.onNewResult(cachedReference, BaseConsumer.simpleStatusForIsLast(isFinal));
        cachedReference.close();
        if (isFinal) {
          return;
        }
      }

      if (producerContext.getLowestPermittedRequestLevel().getValue()
          >= ImageRequest.RequestLevel.BITMAP_MEMORY_CACHE.getValue()) {
        listener.onProducerFinishWithSuccess(
            requestId,
            getProducerName(),
            listener.requiresExtraMap(requestId)
                ? ImmutableMap.of(EXTRA_CACHED_VALUE_FOUND, "false")
                : null);
        listener.onUltimateProducerReached(requestId, getProducerName(), false);
        consumer.onNewResult(null, Consumer.IS_LAST);
        return;
      }

      Consumer<CloseableReference<CloseableImage>> wrappedConsumer =
          wrapConsumer(
              consumer, cacheKey, producerContext.getImageRequest().isMemoryCacheEnabled());
      listener.onProducerFinishWithSuccess(
          requestId,
          getProducerName(),
          listener.requiresExtraMap(requestId)
              ? ImmutableMap.of(EXTRA_CACHED_VALUE_FOUND, "false")
              : null);
      FrescoSystrace.beginSection("mInputProducer.produceResult");
      mInputProducer.produceResults(wrappedConsumer, producerContext);
      FrescoSystrace.endSection();
    } finally {
      FrescoSystrace.endSection();
    }
  }

  protected Consumer<CloseableReference<CloseableImage>> wrapConsumer(
      final Consumer<CloseableReference<CloseableImage>> consumer,
      final CacheKey cacheKey,
      final boolean isMemoryCacheEnabled) {
    return new DelegatingConsumer<
        CloseableReference<CloseableImage>, CloseableReference<CloseableImage>>(consumer) {
      @Override
      public void onNewResultImpl(
          CloseableReference<CloseableImage> newResult, @Status int status) {
        try {
          FrescoSystrace.beginSection("BitmapMemoryCacheProducer#onNewResultImpl");
          final boolean isLast = isLast(status);
          // ignore invalid intermediate results and forward the null result if last
          if (newResult == null) {
            if (isLast) {
              getConsumer().onNewResult(null, status);
            }
            return;
          }
          // stateful and partial results cannot be cached and are just forwarded
          if (newResult.get().isStateful() || statusHasFlag(status, IS_PARTIAL_RESULT)) {
            getConsumer().onNewResult(newResult, status);
            return;
          }
          // if the intermediate result is not of a better quality than the cached result,
          // forward the already cached result and don't cache the new result.
          if (!isLast) {
            CloseableReference<CloseableImage> currentCachedResult = mMemoryCache.get(cacheKey);
            if (currentCachedResult != null) {
              try {
                QualityInfo newInfo = newResult.get().getQualityInfo();
                QualityInfo cachedInfo = currentCachedResult.get().getQualityInfo();
                if (cachedInfo.isOfFullQuality()
                    || cachedInfo.getQuality() >= newInfo.getQuality()) {
                  getConsumer().onNewResult(currentCachedResult, status);
                  return;
                }
              } finally {
                CloseableReference.closeSafely(currentCachedResult);
              }
            }
          }
          // cache, if needed, and forward the new result
          CloseableReference<CloseableImage> newCachedResult = null;
          if (isMemoryCacheEnabled) {
            newCachedResult = mMemoryCache.cache(cacheKey, newResult);
          }
          try {
            if (isLast) {
              getConsumer().onProgressUpdate(1f);
            }
            getConsumer()
                .onNewResult((newCachedResult != null) ? newCachedResult : newResult, status);
          } finally {
            CloseableReference.closeSafely(newCachedResult);
          }
        } finally {
          FrescoSystrace.endSection();
        }
      }
    };
  }

  protected String getProducerName() {
    return PRODUCER_NAME;
  }
}
