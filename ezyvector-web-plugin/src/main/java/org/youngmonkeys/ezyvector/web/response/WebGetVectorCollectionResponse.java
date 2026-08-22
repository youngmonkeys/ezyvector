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

package org.youngmonkeys.ezyvector.web.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WebGetVectorCollectionResponse {

    private Result result;
    private String status;

    @Getter
    @Builder
    public static class Result {
        private Config config;
    }

    @Getter
    @Builder
    public static class Config {
        private Params params;
    }

    @Getter
    @Builder
    public static class Params {
        private Vectors vectors;
    }

    @Getter
    @Builder
    public static class Vectors {
        private long size;
    }
}
