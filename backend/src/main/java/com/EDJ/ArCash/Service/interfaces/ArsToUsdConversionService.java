package com.EDJ.ArCash.Service.interfaces;

import com.EDJ.ArCash.Service.result.ArsToUsdConversion;
import com.EDJ.ArCash.Service.result.DebitPreview;

public interface ArsToUsdConversionService {
    public DebitPreview previewDebit(double amountArs);

    public ArsToUsdConversion calculate(double amountArs);

}
