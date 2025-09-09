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
package io.jrb.labs.modelms.config

import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.commons.eventbus.SystemEventLogger
import io.jrb.labs.modelms.model.ModelEntity
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.index.ReactiveIndexOperations

@Configuration
class ApplicationConfiguration(
    private val mongoTemplate: ReactiveMongoTemplate
) {

    @Bean
    fun systemEventBus(): SystemEventBus = SystemEventBus()

    @Bean
    fun systemEventLogger(systemEventBus: SystemEventBus): SystemEventLogger = SystemEventLogger(systemEventBus)

    @PostConstruct
    fun initIndexes() {
        val indexOps: ReactiveIndexOperations = mongoTemplate.indexOps(ModelEntity::class.java)

        val modelFingerprintIndex = Index()
            .on("model", Sort.Direction.ASC)
            .on("fingerprint", Sort.Direction.ASC)
            .unique()

        // trigger the reactive index creation (must subscribe)
        indexOps.createIndex(modelFingerprintIndex)
            .subscribe { idx -> println("✅ Ensured index created: $idx") }
    }

}