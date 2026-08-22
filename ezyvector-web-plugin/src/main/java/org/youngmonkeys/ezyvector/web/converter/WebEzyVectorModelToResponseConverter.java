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

package org.youngmonkeys.ezyvector.web.converter;

import org.youngmonkeys.ezyvector.model.EzyVectorCollectionModel;
import org.youngmonkeys.ezyvector.web.response.WebGetVectorCollectionResponse;

public class WebEzyVectorModelToResponseConverter {

    public WebGetVectorCollectionResponse toGetVectorCollectionResponse(
        EzyVectorCollectionModel model
    ) {
        return WebGetVectorCollectionResponse.builder()
            .result(
                WebGetVectorCollectionResponse.Result
                    .builder()
                    .config(
                        WebGetVectorCollectionResponse.Config
                            .builder()
                            .params(
                                WebGetVectorCollectionResponse.Params
                                    .builder()
                                    .vectors(
                                        WebGetVectorCollectionResponse.Vectors
                                            .builder()
                                            .size(model.getVectorSize())
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .status("ok")
            .build();
    }
}
