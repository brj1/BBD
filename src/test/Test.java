/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import test.beast.evolution.speciation.BirthDeathSkylineWithDatesTest;

/**
 *
 * @author bjones
 */
public class Test {
    public static void main(String [] args) {
        BirthDeathSkylineWithDatesTest test = new BirthDeathSkylineWithDatesTest();
        
        try {test.testDateChange(); } catch (Exception e) {System.out.println(e);}
    }
}
