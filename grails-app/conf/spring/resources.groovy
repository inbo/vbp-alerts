import grails.util.Holders
import liquibase.integration.spring.SpringLiquibase
import org.springframework.scheduling.quartz.SchedulerFactoryBean
import org.springframework.web.servlet.i18n.CookieLocaleResolver
import java.time.Duration
import au.org.ala.alerts.quartz.AutowiringSpringBeanJobFactory

beans = {
    if (Holders.config.getProperty('spring.liquibase.enabled', Boolean, true)) {
        // configure Liquibase to use GORM dataSource
        liquibase(SpringLiquibase) {
            dataSource = ref('dataSource')
            changeLog = 'classpath:db/changelog/db.changelog-master.xml'
        }
    }

    // VBP: language is shared across portal apps through the 'vbp-lang' cookie
    localeResolver(CookieLocaleResolver, Holders.config.getProperty('i18n.cookieName', String, 'vbp-lang')) {
        defaultLocale = Locale.forLanguageTag(Holders.config.getProperty('siteDefaultLanguage', String, 'nl'))
        cookieMaxAge = Duration.ofDays(365)
        cookieHttpOnly = false
        cookieSecure = true
    }


    // This tells Spring to find all @Component, @Service, etc. in your package
    // Assure the Quartz schedulers are loaded

    xmlns context: "http://www.springframework.org/schema/context"
    context.'component-scan'('base-package': "au.org.ala.alerts.quartz")


    // 1. Define the custom Quartz JobFactory
    quartzJobFactory(AutowiringSpringBeanJobFactory)

    // 2. Link it to the Scheduler
    quartzScheduler(SchedulerFactoryBean) {
        dataSource = ref('dataSource')
        transactionManager = ref('transactionManager')
        jobFactory = ref('quartzJobFactory') // CRITICAL: This enables @Autowired in jobs

        quartzProperties = [
                'org.quartz.scheduler.instanceName': 'quartzScheduler',
                'org.quartz.jobStore.class': 'org.springframework.scheduling.quartz.LocalDataSourceJobStore',
                'org.quartz.jobStore.driverDelegateClass': 'org.quartz.impl.jdbcjobstore.StdJDBCDelegate',
                'org.quartz.jobStore.tablePrefix': 'QRTZ_'
        ] as Properties
    }

}
