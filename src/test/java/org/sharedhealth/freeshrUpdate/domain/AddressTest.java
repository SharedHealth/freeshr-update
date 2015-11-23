package org.sharedhealth.freeshrUpdate.domain;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AddressTest {

    @Test
    public void shouldCheckIfAddressDataIsEmpty(){
        Address emptyAddress = new Address();
        assertTrue(emptyAddress.isEmpty());
    }

    @Test
    public void shouldCheckIfAddressDataIsNotEmpty(){
        Address someAddress = new Address();
        someAddress.setAddressLine("No 65");
        assertFalse(someAddress.isEmpty());
    }


}