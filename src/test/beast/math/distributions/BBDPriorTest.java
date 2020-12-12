package test.beast.math.distributions;

import org.junit.Test;

import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.math.distributions.BBDPrior;
import beast.math.distributions.Exponential;
import beast.util.TreeParser;
import junit.framework.TestCase;

public class BBDPriorTest extends TestCase {

    @Test
    /*
    Current DOES NOT work
    */
    public void testBBDTimePrior() throws Exception {
        Alignment data = new Alignment();
        TreeParser tree = new TreeParser();
        tree.initByName("taxa", data,
                "newick", "((human:0.024003,(chimp:0.010772,bonobo:0.010772):0.013231):0.012035," +
                "(gorilla:0.024003,(orangutan:0.010772,siamang:0.010772):0.013231):0.012035);",
                "IsLabelledNewick", true);

        Taxon human = new Taxon();
        human.setID("human");
        Taxon bonobo = new Taxon();
        bonobo.setID("bonobo");
        Taxon chimp = new Taxon();
        chimp.setID("chimp");
        Taxon gorilla = new Taxon();
        gorilla.setID("gorilla");
        Taxon orangutan = new Taxon();
        orangutan.setID("orangutan");
        Taxon siamang = new Taxon();
        siamang.setID("siamang");
        
        BBDPrior prior = new BBDPrior();
        
        TaxonSet set = new TaxonSet();
        set.initByName("taxon", human, "taxon", bonobo, "taxon", chimp);
        Exponential exp = new Exponential();
        prior.initByName("tree", tree, "taxonset", set, "distr", exp, "startDateProbability", 0.01);
        
        assertTrue(tree.hasDateTrait());
        
        double logP = prior.calculateLogP();
        assertEquals(Double.NEGATIVE_INFINITY, logP, 0);
    }
    
    @Test
    public void testBBDTimePriorWithOperators() throws Exception {
        
    }
}
