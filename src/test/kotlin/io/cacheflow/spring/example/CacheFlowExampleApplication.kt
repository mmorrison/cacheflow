package io.cacheflow.spring.example

import io.cacheflow.spring.annotation.CacheFlow
import io.cacheflow.spring.annotation.CacheFlowEvict
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.stereotype.Service

/**
 * Example application demonstrating CacheFlow usage.
 */
@SpringBootApplication
class CacheFlowExampleApplication : CommandLineRunner {

    /**
     * Example service demonstrating cache operations.
     */
    @Service
    class ExampleService {
        private val simulationDelayMs = 1_000L

        /**
         * Retrieves expensive data with caching.
         *
         * @param id The data identifier
         * @return The expensive data
         */
        @CacheFlow(key = "#id", ttl = 30L)
        fun getExpensiveData(id: Long): String {
            println("Computing expensive data for id: $id")
            Thread.sleep(simulationDelayMs) // Simulate expensive operation
            return "Expensive data for id: $id"
        }

        /**
         * Updates data and evicts cache.
         *
         * @param id The data identifier
         * @param newData The new data value
         */
        @CacheFlowEvict(key = "#id")
        fun updateData(id: Long, newData: String) {
            println("Updating data for id: $id with: $newData")
        }
    }

    /**
     * Runs the example application.
     *
     * @param args Command line arguments
     */
    override fun run(vararg args: String?) {
        val service =
            SpringApplication.run(CacheFlowExampleApplication::class.java, *args)
                .getBean(ExampleService::class.java)

        println("=== CacheFlow Example ===")

        // First call - will compute and cache
        println("First call:")
        val start1 = System.currentTimeMillis()
        val result1 = service.getExpensiveData(1L)
        val time1 = System.currentTimeMillis() - start1
        println("Result: $result1 (took ${time1}ms)")

        // Second call - should be cached
        println("\nSecond call (should be cached):")
        val start2 = System.currentTimeMillis()
        val result2 = service.getExpensiveData(1L)
        val time2 = System.currentTimeMillis() - start2
        println("Result: $result2 (took ${time2}ms)")

        // Evict cache
        println("\nEvicting cache...")
        service.updateData(1L, "New data")

        // Third call - should compute again
        println("\nThird call (after eviction):")
        val start3 = System.currentTimeMillis()
        val result3 = service.getExpensiveData(1L)
        val time3 = System.currentTimeMillis() - start3
        println("Result: $result3 (took ${time3}ms)")

        println("\n=== Example Complete ===")
    }
}

/**
 * Main function to run the example application.
 *
 * @param args Command line arguments
 */
fun main(args: Array<String>) {
    SpringApplication.run(CacheFlowExampleApplication::class.java, *args)
}
