
package hello;

import javax.jms.ConnectionFactory;

import com.uber.jaeger.Configuration;
import com.uber.jaeger.samplers.ProbabilisticSampler;
import io.opentracing.Tracer;
import io.opentracing.contrib.jms.spring.TracingJmsTemplate;
import io.opentracing.contrib.jms.spring.TracingMessageConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import brave.Tracing;
import brave.opentracing.BraveTracer;
import zipkin.Span;
import zipkin.reporter.AsyncReporter;
import zipkin.reporter.okhttp3.OkHttpSender;

@SpringBootApplication
@EnableJms
public class Application {

    @Bean
    public io.opentracing.Tracer jaegerTracer() {
        return new Configuration("spring-boot-jms", new Configuration.SamplerConfiguration(ProbabilisticSampler.TYPE, 1),
                new Configuration.ReporterConfiguration())
                .getTracer();
    }

    //TBD:  brave-opentracing uses opentracing-api-0.21.0 which is not
    //      compatible with the opentracing-api-0.30 which opentracing-jms uses

//	@Bean
//	public io.opentracing.Tracer zipkinTracer() {
//		OkHttpSender okHttpSender = OkHttpSender.create("http://localhost:9411/api/v1/spans");
//		AsyncReporter<Span> reporter = AsyncReporter.builder(okHttpSender).build();
//		Tracing braveTracer = Tracing.newBuilder().localServiceName("spring-boot-jms").reporter(reporter).build();
//		return BraveTracer.create(braveTracer);
//	}


    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory, io.opentracing.Tracer tracer,
                                   MessageConverter tracingJmsMessageConverter) {
        JmsTemplate jmsTemplate =new TracingJmsTemplate(connectionFactory, tracer);
        jmsTemplate.setMessageConverter(tracingJmsMessageConverter);
        return jmsTemplate;
    }

    @Bean
    public MessageConverter tracingJmsMessageConverter(Tracer tracer) {
        return new TracingMessageConverter(jacksonJmsMessageConverter(), tracer);
    }



    @Bean
    public JmsListenerContainerFactory<?> myFactory(ConnectionFactory connectionFactory,
                                                    DefaultJmsListenerContainerFactoryConfigurer configurer,
                                                    MessageConverter tracingJmsMessageConverter) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setMessageConverter(tracingJmsMessageConverter);
        // This provides all boot's default to this factory, including the message converter
        configurer.configure(factory, connectionFactory);
        // You could still override some of Boot's default if necessary.
        return factory;
    }

    //@Bean // Serialize message content to json using TextMessage
    public static MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

    public static void main(String[] args) {
        // Launch the application
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);

        JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);

        // Send a message with a POJO - the template reuse the message converter
        System.out.println("Sending an email message.");
        jmsTemplate.convertAndSend("mailbox", new Email("info@example.com", "Hello"));
    }

}
