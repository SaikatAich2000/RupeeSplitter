plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.sonarqube)
}

// Server URL and token are supplied at run time (SONAR_HOST_URL / SONAR_TOKEN), never stored here.
sonar {
    properties {
        property("sonar.projectKey", "rupee-splitter")
        property("sonar.projectName", "Rupee Splitter")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            listOf(
                "app/build/reports/coverage/test/debug/report.xml",
                "app/build/reports/coverage/androidTest/debug/connected/report.xml"
            ).joinToString(",") { rootDir.resolve(it).path }
        )
    }
}
