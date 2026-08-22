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

package org.youngmonkeys.ezyvector.service;

import lombok.AllArgsConstructor;
import org.youngmonkeys.ezyvector.converter.EzyVectorEntityToModelConverter;
import org.youngmonkeys.ezyvector.model.EzyVectorCollectionModel;
import org.youngmonkeys.ezyvector.repo.EzyVectorCollectionRepository;

@AllArgsConstructor
public class EzyVectorCollectionService {

    private final EzyVectorCollectionRepository collectionRepository;
    private final EzyVectorEntityToModelConverter entityToModelConverter;

    public EzyVectorCollectionModel getCollectionByName(
        String collectionName
    ) {
        return entityToModelConverter.toModel(
            collectionRepository.findByName(collectionName)
        );
    }
}
