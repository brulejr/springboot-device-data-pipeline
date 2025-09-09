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
package io.jrb.labs.modelms.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.commons.service.ControllableService
import io.jrb.labs.commons.service.CrudOutcome
import io.jrb.labs.messages.Rtl433Message
import io.jrb.labs.modelms.model.ModelEntity
import io.jrb.labs.modelms.repository.ModelRepository
import io.jrb.labs.modelms.resource.SensorsUpdateRequest
import io.jrb.labs.resources.model.ModelResource
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@Service
class ModelService(
    private val modelRepository: ModelRepository,
    private val objectMapper: ObjectMapper,
    systemEventBus: SystemEventBus
) : ControllableService(systemEventBus) {

    suspend fun findModelResource(modelName: String): CrudOutcome<ModelResource> {
        return try {
            val resource = modelRepository.findByModel(modelName)
                .map { it.toModelResource(objectMapper) }
                .awaitFirst()
            if (resource == null) {
                CrudOutcome.NotFound(modelName)
            } else {
                CrudOutcome.Success(resource)
            }
        } catch (e: Exception) {
            CrudOutcome.Error("Failed to find model resource", e)
        }
    }

    suspend fun processRawMessage(rtl433Message: Rtl433Message): ModelResource? {
        val modelName = rtl433Message.payload.model
        val jsonString = objectMapper.writeValueAsString(rtl433Message)
        val fingerprint = fingerprint(jsonString)

        val foundModel = modelRepository.findByModel(modelName)
            .filter { it.fingerprint != fingerprint }
            .map { it.copy(jsonStructure = jsonString, fingerprint = fingerprint).withUpdateInfo() }
            .awaitSingleOrNull()

        val updatedModel = foundModel ?: ModelEntity(
            source = rtl433Message.source.toString(),
            model = modelName,
            jsonStructure = jsonString,
            fingerprint = fingerprint
        ).withCreateInfo()

        return modelRepository.save(updatedModel)
            .map { it.toModelResource(objectMapper) }
            .awaitFirst()
    }

    suspend fun retrieveModelResources(): CrudOutcome<List<ModelResource>> {
        return try {
            val resources = modelRepository.findAll()
                .map { it.toModelResource(objectMapper) }
                .collectList()
                .awaitSingleOrNull() ?: emptyList()
            CrudOutcome.Success(resources)
        } catch (e: Exception) {
            CrudOutcome.Error("Failed to retrieve model resources", e)
        }
    }

    suspend fun updateSensors(modelName: String, request: SensorsUpdateRequest): CrudOutcome<ModelResource> {
        return try {
            val updatedModel = modelRepository.findByModel(modelName)
                .map { it.copy(
                    category = request.category,
                    sensors = request.sensors?.map { it.toSensorMapping() }
                ) }
                .flatMap { modelRepository.save(it) }
                .map { it.toModelResource(objectMapper) }
                .awaitFirst()
            if (updatedModel == null) {
                CrudOutcome.NotFound(modelName)
            } else {
                CrudOutcome.Success(updatedModel)
            }
        } catch (e: Exception) {
            CrudOutcome.Error("Failed to find model resource", e)
        }
    }

    private fun fingerprint(input: String, algorithm: String = "SHA-256"): String {
        val bytes = MessageDigest.getInstance(algorithm).digest(input.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

}