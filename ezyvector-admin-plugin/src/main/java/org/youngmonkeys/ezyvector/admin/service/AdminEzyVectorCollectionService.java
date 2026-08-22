package org.youngmonkeys.ezyvector.admin.service;

import com.tvd12.ezyhttp.server.core.annotation.Service;
import org.youngmonkeys.ezyvector.admin.converter.AdminEzyVectorEntityToModelConverter;
import org.youngmonkeys.ezyvector.admin.repo.AdminEzyVectorCollectionRepository;
import org.youngmonkeys.ezyvector.service.EzyVectorCollectionService;

@Service
public class AdminEzyVectorCollectionService extends EzyVectorCollectionService {

    public AdminEzyVectorCollectionService(
        AdminEzyVectorCollectionRepository collectionRepository,
        AdminEzyVectorEntityToModelConverter entityToModelConverter
    ) {
        super(
            collectionRepository,
            entityToModelConverter
        );
    }
}
