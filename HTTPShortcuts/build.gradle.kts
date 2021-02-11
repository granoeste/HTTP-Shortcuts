buildscript {
    repositories {
        jcenter()
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.0.1")
        classpath("io.realm:realm-gradle-plugin:7.0.8")
        classpath("com.bugsnag:bugsnag-android-gradle-plugin:4.+")
        classpath("org.jetbrains:markdown:0.1.45")
        classpath(kotlin("gradle-plugin", "1.4.30"))
    }
}

ext {
    set("bugsnagAPIKey", System.getenv("BUGSNAG_API_KEY") ?: "")
    set("buildTimestamp", java.util.Date().time.toString())
}

allprojects {
    repositories {
        jcenter()
        maven("https://jitpack.io")
    }
}
repositories {
    mavenCentral()
}

tasks.register("syncChangeLog") {
    description = "copies the CHANGELOG.md file's content into the app so it can be displayed"

    doLast {
        val changelogMarkdown = File("../CHANGELOG.md").readText()
        val template = File("changelog_template.html").readText()
        val flavour = org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor()
        val parsedTree = org.intellij.markdown.parser.MarkdownParser(flavour)
            .buildMarkdownTreeFromString(changelogMarkdown)
        val html = org.intellij.markdown.html.HtmlGenerator(changelogMarkdown, parsedTree, flavour).generateHtml()
        File("app/src/main/assets/changelog.html").writeText(
            template.replace("<!-- CONTENT -->", html)
        )
    }
}

