Heros IFDS/IDE Solver
=====================

Heros is a generic implementation of an IFDS/IDE Solver that can be plugged into existing, Java-based program analysis frameworks. A reference connector exists for the [Soot](http://www.sable.mcgill.ca/soot/) framework.

Who are the developers of Heros?
--------------------------------
Heros was developed and is maintained by [Eric Bodden](http://bodden.de/).

Why is Heros called Heros?
--------------------------
The name contains (in a different order) the first characters of the last names Reps, Horwitz and Sagiv, the original inventors of the IFDS/IDE frameworks.

What is IFDS/IDE in the first place?
------------------------------------
IFDS is a general framework for solving inter-procedural, finite, distributive subset problems in a flow-sensitive, fully context-sensitive manner. From a user's perspective, IFDS allows static program analysis in a template-driven manner. Users simply define flow functions for an analysis problem but don't need to worry about solving the analysis problem. The latter is automatically taken care of by the solver, in this case by Heros.
IDE is an extension of IFDS that allows more expressive computations. Heros implements an IDE solver and supports IFDS problems as special cases of IDE.

What are the unique features of Heros over other IFDS/IDE solvers?
------------------------------------------------------------------
To the best of our knowledge there exist at least two other similar solvers implemented in Java. [Wala](http://wala.sf.net/) implements a solver that supports IFDS but not IDE. The solver is highly scalable but in our eyes requires more verbose definitions of client analyses. Heros is fully multi-threaded, while Wala's solver is not. There also exists a Scala-based solver by [Nomair A. Naeem, Ondˇrej Lhota ́k, and Jonathan Rodriguez](http://dx.doi.org/10.1007/978-3-642-11970-5_8). This implementation does support IDE, and there exists a multi-threaded version of it, but as of yet the implementation is not publicly available.

Under what License can I use Heros?
-----------------------------------
Heros is released under LGPL - see [LICENSE.txt](LICENSE.txt) for details.