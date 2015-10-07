package bullet.impl;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.auto.service.AutoService;

import java.util.Collections;

import javax.annotation.processing.Processor;
import javax.lang.model.SourceVersion;

@AutoService(Processor.class)
public class ComponentProcessor extends BasicAnnotationProcessor {

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  protected Iterable<? extends ProcessingStep> initSteps() {
    return Collections.singleton(new ComponentProcessingStep(processingEnv));
  }
}
