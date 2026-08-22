/*
 * Copyright 2026 youngmonkeys.org
 *
 * Licensed under the ezyplatform, Version 1.0.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://youngmonkeys.org/licenses/ezyplatform-1.0.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.youngmonkeys.ezyvector.web.controller.api;

import com.tvd12.ezyfox.util.EzyLoggable;
import com.tvd12.ezyhttp.core.constant.StatusCodes;
import com.tvd12.ezyhttp.core.exception.HttpBadRequestException;
import com.tvd12.ezyhttp.core.exception.HttpNotFoundException;
import com.tvd12.ezyhttp.core.exception.HttpUnauthorizedException;
import com.tvd12.ezyhttp.core.response.ResponseEntity;
import com.tvd12.ezyhttp.server.core.annotation.Api;
import com.tvd12.ezyhttp.server.core.annotation.Controller;
import com.tvd12.ezyhttp.server.core.annotation.DoGet;
import com.tvd12.ezyhttp.server.core.annotation.DoPost;
import com.tvd12.ezyhttp.server.core.annotation.DoPut;
import com.tvd12.ezyhttp.server.core.annotation.PathVariable;
import com.tvd12.ezyhttp.server.core.annotation.RequestBody;
import com.tvd12.ezyhttp.server.core.annotation.TryCatch;
import com.tvd12.ezyhttp.server.core.request.RequestArguments;
import lombok.AllArgsConstructor;
import org.youngmonkeys.ezyplatform.exception.ResourceNotFoundException;
import org.youngmonkeys.ezyvector.web.controller.service.WebEzyVectorCollectionControllerService;
import org.youngmonkeys.ezyvector.web.controller.service.WebEzyVectorPointsControllerService;
import org.youngmonkeys.ezyvector.web.controller.service.WebEzyVectorSearchControllerService;
import org.youngmonkeys.ezyvector.web.converter.WebEzyVectorRequestToModelConverter;
import org.youngmonkeys.ezyvector.web.request.WebCreateVectorCollectionRequest;
import org.youngmonkeys.ezyvector.web.request.WebEzyVectorSearchRequest;
import org.youngmonkeys.ezyvector.web.request.WebUpsertVectorPointsRequest;
import org.youngmonkeys.ezyvector.web.response.WebCreateVectorCollectionResponse;
import org.youngmonkeys.ezyvector.web.response.WebEzyVectorSearchResponse;
import org.youngmonkeys.ezyvector.web.response.WebGetVectorCollectionResponse;
import org.youngmonkeys.ezyvector.web.response.WebUpsertVectorPointsResponse;
import org.youngmonkeys.ezyvector.web.validator.WebEzyVectorCollectionValidator;
import org.youngmonkeys.ezyvector.web.validator.WebEzyVectorPointsValidator;
import org.youngmonkeys.ezyvector.web.validator.WebEzyVectorSearchValidator;

@Api
@Controller("/ezyvector/collections")
@AllArgsConstructor
public class WebApiEzyVectorCollectionController extends EzyLoggable {

    private final WebEzyVectorCollectionControllerService vectorCollectionControllerService;
    private final WebEzyVectorPointsControllerService vectorPointsControllerService;
    private final WebEzyVectorSearchControllerService vectorSearchControllerService;
    private final WebEzyVectorCollectionValidator vectorCollectionValidator;
    private final WebEzyVectorPointsValidator vectorPointsValidator;
    private final WebEzyVectorSearchValidator vectorSearchValidator;
    private final WebEzyVectorRequestToModelConverter vectorRequestToModelConverter;

    @DoPut("/{collectionName}")
    public WebCreateVectorCollectionResponse collectionNamePut(
        RequestArguments arguments,
        @PathVariable String collectionName,
        @RequestBody WebCreateVectorCollectionRequest request
    ) throws Exception {
        vectorCollectionValidator.validateAuthentication(arguments);
        vectorCollectionValidator.validate(request);
        vectorCollectionControllerService.createCollectionIfAbsent(
            collectionName,
            vectorRequestToModelConverter.toSaveVectorCollectionModel(
                request
            )
        );
        return WebCreateVectorCollectionResponse.builder()
            .result(Boolean.TRUE)
            .status("ok")
            .build();
    }

    @DoGet("/{collectionName}")
    public WebGetVectorCollectionResponse collectionNameGet(
        RequestArguments arguments,
        @PathVariable String collectionName
    ) {
        vectorCollectionValidator.validateAuthentication(arguments);
        return vectorCollectionControllerService
            .getVectorCollectionByName(collectionName);
    }

    @DoPut("/{collectionName}/points")
    public WebUpsertVectorPointsResponse collectionNamePointsPut(
        RequestArguments arguments,
        @PathVariable String collectionName,
        @RequestBody WebUpsertVectorPointsRequest request
    ) throws Exception {
        vectorCollectionValidator.validateAuthentication(arguments);
        vectorPointsValidator.validate(request);
        return vectorPointsControllerService.upsertPoints(
            collectionName,
            request
        );
    }

    @DoPost("/{collectionName}/points/search")
    public WebEzyVectorSearchResponse collectionNamePointsSearchPost(
        RequestArguments arguments,
        @PathVariable String collectionName,
        @RequestBody WebEzyVectorSearchRequest request
    ) throws Exception {
        vectorCollectionValidator.validateAuthentication(arguments);
        vectorSearchValidator.validate(request);
        return vectorSearchControllerService.search(
            collectionName,
            request
        );
    }

    @TryCatch(HttpBadRequestException.class)
    public Object handle(
        HttpBadRequestException e
    ) {
        logger.info("{}({})", e.getClass().getSimpleName(), e.getMessage());
        return ResponseEntity.badRequest(e.getData());
    }

    @TryCatch(ResourceNotFoundException.class)
    public Object handle(
        ResourceNotFoundException e
    ) {
        logger.info("{}({})", e.getClass().getSimpleName(), e.getMessage());
        return ResponseEntity.notFound(
            e.getResponseData()
        );
    }

    @TryCatch(HttpNotFoundException.class)
    public Object handle(
        HttpNotFoundException e
    ) {
        logger.info("{}({})", e.getClass().getSimpleName(), e.getMessage());
        return ResponseEntity.notFound(e.getData());
    }

    @TryCatch(HttpUnauthorizedException.class)
    public Object handle(
        HttpUnauthorizedException e
    ) {
        logger.info("{}({})", e.getClass().getSimpleName(), e.getMessage());
        return ResponseEntity.builder()
            .status(StatusCodes.UNAUTHORIZED)
            .body(e.getData())
            .build();
    }
}
