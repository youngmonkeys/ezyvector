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

package org.youngmonkeys.ezyvector.constant;

import com.tvd12.ezyfox.util.EzyEnums;

import java.util.Map;

public enum EzyVectorDistance {
    COSINE;

    private static final Map<String, EzyVectorDistance> MAP_BY_NAME =
        EzyEnums.enumMap(
            EzyVectorDistance.class,
            EzyVectorDistance::toString
        );

    public static EzyVectorDistance of(
        String value
    ) {
        return value == null
            ? null
            : MAP_BY_NAME.get(value.toUpperCase());
    }

    public boolean equalsValue(String value) {
        return toString().equals(value);
    }
}
