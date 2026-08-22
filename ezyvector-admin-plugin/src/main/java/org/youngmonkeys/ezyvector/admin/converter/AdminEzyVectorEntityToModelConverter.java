package org.youngmonkeys.ezyvector.admin.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import org.youngmonkeys.ezyplatform.time.ClockProxy;
import org.youngmonkeys.ezyvector.converter.EzyVectorEntityToModelConverter;

@EzySingleton
public class AdminEzyVectorEntityToModelConverter
    extends EzyVectorEntityToModelConverter {

    public AdminEzyVectorEntityToModelConverter(
        ClockProxy clock,
        ObjectMapper objectMapper
    ) {
        super(clock, objectMapper);
    }
}
