package com.carbonorm.carbonjava.abstracts;

import com.carbonorm.carbonjava.interfaces.ICarbonJAVA;
import com.carbonorm.carbonjava.CarbonJAVA;
import com.carbonorm.carbonjava.throwables.PublicAlert;
import com.carbonorm.carbonjava.throwables.ThrowableHandler;

public class RestQueryValidation extends RestAutoTargeting {

    protected void checkPrefix()  {

        String tablePrefix = getFieldValue("TABLE_PREFIX").toString();

        String prefix = CarbonJAVA.configuration.get(ICarbonJAVA.REST).get(ICarbonJAVA.TABLE_PREFIX);

        if (prefix == null) {
            prefix = "";
        }

        if (!prefix.equals(tablePrefix)) {
            try {

                throw new PublicAlert("The tables prefix (" + CarbonJAVA.TABLE_PREFIX + ") does not match the one (" + prefix + ") found in your configuration (" + CarbonJAVA.configuration.toString() + "). Please make sure you've initiated CarbonPHP before trying to run restful operations. Otherwise, you may need to rebuild rest.");

            } catch (PublicAlert e) {

                ThrowableHandler.generateLog(e);

            }

        }

    }


}
