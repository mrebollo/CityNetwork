package density

import scala.util.Random

class PADGeneratorLauncher {

  // static mutable fields to get indicators

  var moran: Double = 0
  var distance: Double = 0
  var entropy: Double = 0
  var slope: Double = 0
  var rsquared: Double = 0

  def main(worldwidth: Int, pop: Double, diff: Double, diffSteps: Int, growth: Double, alpha: Double, replication: Int) = {

    println("Params : " + pop + " ; " + diff + " ; " + diffSteps + " ; " + growth + " ; " + alpha + " ; " + replication)

    //val UIR = (pop.toString + "_" + diff + "_" + diffSteps + "_" + growth + "_" + alpha + "_" + replication)
    //println(UIR)
    // replication and diffsteps should be enough in 'small' explorations,
    // easier to retrieve later (exact int value)
    val UIR = replication.toString + diffSteps.toString

    val t = System.currentTimeMillis()

    implicit val rng = new Random

    val gen = new PrefAttDiffusionGenerator {
      override def size: Int = worldwidth
      override def totalPopulation: Double = pop
      override def diffusion: Double = diff
      override def diffusionSteps: Int = diffSteps
      override def growthRate: Double = growth
      override def alphaAtt: Double = alpha
      override def temp_file: String = "tmp/pop_" + UIR + ".csv"
    }

    // compute
    val world = gen.world(rng)
    gen.export_static(world)

    /*
    Computation of indicators using R ;
    Very painful in comp time - Why ? (pb with processor load) -

    //println("Ellapsed Time generation : " + (System.currentTimeMillis() - t) / 1000.0)
    //val s = new File("./").getAbsolutePath()
    //println(s)
    // external call to compute indicators
    //("R -e source('csv2raster.R',chdir=TRUE);exportIndics('" + UIR + "')").!

    //val s = new File("./").getAbsolutePath()
    //println(s.substring(0, s.length() - 1))

    //get back indicators
    //val r = new BufferedReader(new FileReader("tmp/temp_indics_" + UIR + ".csv"))
    //r.readLine();
    //val indics = r.readLine().split(";")
    */

    //moran = Morphology.moran(world)
    //distance = Morphology.distanceMean(world)
    entropy = Morphology.entropy(world)
    val slopeVals = Morphology.slope(world)
    slope = slopeVals._1
    rsquared = slopeVals._2

    println(gen.temp_file)
    println("Indicators : Moran = " + moran + " ; D = " + distance + " ; E = " + entropy + " ; alpha = " + slope + " ; R2 = " + rsquared)
    println("Ellapsed Time : " + (System.currentTimeMillis() - t) / 1000.0)

  }

}
