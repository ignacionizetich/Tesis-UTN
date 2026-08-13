package com.EDJ.ArCash.Service.interfaces;

import com.EDJ.ArCash.Service.result.UsdDebitPreview;
import com.EDJ.ArCash.Service.result.UsdToArsConversion;

public interface UsdToArsConversionService {
    public UsdDebitPreview previewDebit(double amountUsd);

    public UsdToArsConversion calculate(double amountUsd);

}
