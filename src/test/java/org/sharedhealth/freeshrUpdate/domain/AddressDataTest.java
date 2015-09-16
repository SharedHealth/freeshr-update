package org.sharedhealth.freeshrUpdate.domain;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AddressDataTest  {

    @Test
    public void shouldCheckIfAddressDataIsEmpty(){
        AddressData emptyAddress = new AddressData();
        assertTrue(emptyAddress.isEmpty());
    }

    @Test
    public void shouldCheckIfAddressDataIsNotEmpty(){
        AddressData someAddress = new AddressData();
        someAddress.setAddressLine("No 65");
        assertFalse(someAddress.isEmpty());
    }


}