/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Jon Brule <brulejr@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.jrb.labs.recommendationms.config

import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.commons.eventbus.SystemEventLogger
import io.jrb.labs.recommendationms.datafill.RecommendationDatafill
import io.jrb.labs.recommendationms.model.FingerprintCount
import io.jrb.labs.recommendationms.model.Recommendation
import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.Index
import java.time.Duration

@Configuration
@EnableConfigurationProperties(RecommendationDatafill::class)
class ApplicationConfiguration(
    private val mongoTemplate: ReactiveMongoTemplate
) {

    @Bean
    fun systemEventBus(): SystemEventBus = SystemEventBus()

    @Bean
    fun systemEventLogger(systemEventBus: SystemEventBus): SystemEventLogger = SystemEventLogger(systemEventBus)

    @PostConstruct
    fun initFingerprintIndexes() {
        val index = Index()
            .on("bucketStartEpoch", Sort.Direction.ASC)
            .expire(Duration.ofDays(1))
        mongoTemplate
            .indexOps(FingerprintCount::class.java)
            .createIndex(index)
            .subscribe { idx -> println("✅ Ensured fingerprint indexes created: $idx") }
    }

    @PostConstruct
    fun initRecommendationIndexes() {
        val index = Index()
            .on("fingerprint", Sort.Direction.ASC)
            .unique()
        mongoTemplate
            .indexOps(Recommendation::class.java)
            .createIndex(index)
            .subscribe { idx -> println("✅ Ensured recommendation indexes created: $idx") }
    }

}