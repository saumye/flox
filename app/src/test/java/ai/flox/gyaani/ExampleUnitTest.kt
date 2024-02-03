package ai.flox

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun test_reorg() {
        val res = ai.flox.Reorg()
        val e1 = Reorg.EmployeeMetadata("Saumye")
        val e2 = Reorg.EmployeeMetadata("Arpit")
        val e3 = Reorg.EmployeeMetadata("Sri")
        val e4 = Reorg.EmployeeMetadata("Kalpesh")
        val e5 = Reorg.EmployeeMetadata("Nitin")
        res.addEmployee(e3, null)
        res.addEmployee(e2, e3)
        res.addEmployee(e4, e3)
        res.addEmployee(e1, e2)
        res.addEmployee(e5, e2)
        res.print(e3)
        res.moveEmployee(e4, e2)
        res.print(e3)
    }
}