/*
 * Copyright 2000-2005 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.uiDesigner.compiler;

import com.intellij.uiDesigner.lw.LwComponent;
import com.intellij.uiDesigner.lw.LwContainer;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: yole
 * Date: 24.11.2005
 * Time: 18:31:19
 * To change this template use File | Settings | File Templates.
 */
public class GridBagLayoutCodeGenerator extends LayoutCodeGenerator {
  private static Type myGridBagLayoutType = Type.getType(GridBagLayout.class);
  private static Type myGridBagConstraintsType = Type.getType(GridBagConstraints.class);

  private Map myIdToConstraintsMap = new HashMap();

  public void generateContainerLayout(final LwComponent lwComponent, final GeneratorAdapter generator, final int componentLocal) {
    if (lwComponent instanceof LwContainer) {
      LwContainer container = (LwContainer) lwComponent;
      if (container.isGrid()) {
        generator.loadLocal(componentLocal);

        generator.newInstance(myGridBagLayoutType);
        generator.dup();
        generator.invokeConstructor(myGridBagLayoutType, Method.getMethod("void <init> ()"));

        generator.invokeVirtual(Type.getType(Container.class), Method.getMethod("void setLayout(java.awt.LayoutManager)"));

        prepareConstraints(container);
      }
    }
  }

  private void prepareConstraints(final LwContainer container) {
    GridLayoutManager gridLayout = (GridLayoutManager) container.getLayout();
    GridBagConverter converter = new GridBagConverter(gridLayout.getMargin());
    for(int i=0; i<container.getComponentCount(); i++) {
      final LwComponent component = (LwComponent)container.getComponent(i);
      converter.addComponent(null, component.getConstraints());
    }
    GridBagConverter.Result[] results = converter.convert();
    for(int i=0; i<results.length; i++) {
      final LwComponent component = (LwComponent)container.getComponent(i);
      myIdToConstraintsMap.put(component.getId(), results [i]);
    }
  }

  public void generateComponentLayout(final LwComponent lwComponent,
                                      final GeneratorAdapter generator,
                                      final int componentLocal,
                                      final int parentLocal) {
    GridBagConverter.Result result = (GridBagConverter.Result) myIdToConstraintsMap.get(lwComponent.getId());
    if (result != null) {
      checkSetSize(generator, componentLocal, "setMinimumSize", result.minimumSize);
      checkSetSize(generator, componentLocal, "setPreferredSize", result.preferredSize);
      checkSetSize(generator, componentLocal, "setMaximumSize", result.maximumSize);

      int gbcLocal = generator.newLocal(myGridBagConstraintsType);

      generator.newInstance(myGridBagConstraintsType);
      generator.dup();
      generator.invokeConstructor(myGridBagConstraintsType, Method.getMethod("void <init> ()"));
      generator.storeLocal(gbcLocal);

      GridBagConstraints defaults = new GridBagConstraints();
      GridBagConstraints constraints = result.constraints;
      if (defaults.gridx != constraints.gridx) {
        setIntField(generator, gbcLocal, "gridx", constraints.gridx);
      }
      if (defaults.gridy != constraints.gridy) {
        setIntField(generator, gbcLocal, "gridy", constraints.gridy);
      }
      if (defaults.gridwidth != constraints.gridwidth) {
        setIntField(generator, gbcLocal, "gridwidth", constraints.gridwidth);
      }
      if (defaults.gridheight != constraints.gridheight) {
        setIntField(generator, gbcLocal, "gridheight", constraints.gridheight);
      }
      if (defaults.weightx != constraints.weightx) {
        setDoubleField(generator, gbcLocal, "weightx", constraints.weightx);
      }
      if (defaults.weighty != constraints.weighty) {
        setDoubleField(generator, gbcLocal, "weighty", constraints.weighty);
      }
      if (defaults.anchor != constraints.anchor) {
        setIntField(generator, gbcLocal, "anchor", constraints.anchor);
      }
      if (defaults.fill != constraints.fill) {
        setIntField(generator, gbcLocal, "fill", constraints.fill);
      }
      if (defaults.ipadx != constraints.ipadx) {
        setIntField(generator, gbcLocal, "ipadx", constraints.ipadx);
      }
      if (defaults.ipady != constraints.ipady) {
        setIntField(generator, gbcLocal, "ipady", constraints.ipady);
      }
      if (!defaults.insets.equals(constraints.insets)) {
        generator.loadLocal(gbcLocal);
        AsmCodeGenerator.pushPropValue(generator, "java.awt.Insets", constraints.insets);
        generator.putField(myGridBagConstraintsType, "insets", Type.getType(Insets.class));
      }

      generator.loadLocal(parentLocal);
      generator.loadLocal(componentLocal);
      generator.loadLocal(gbcLocal);

      generator.invokeVirtual(Type.getType(Container.class), Method.getMethod("void add(java.awt.Component,java.lang.Object)"));
    }
  }

  private void checkSetSize(final GeneratorAdapter generator, final int componentLocal, final String methodName, final Dimension dimension) {
    if (dimension != null) {
      generator.loadLocal(componentLocal);
      AsmCodeGenerator.pushPropValue(generator, "java.awt.Dimension", dimension);
      generator.invokeVirtual(Type.getType(Component.class),
                              new Method(methodName, Type.VOID_TYPE, new Type[] { Type.getType(Dimension.class) }));
    }
  }

  private void setIntField(final GeneratorAdapter generator, final int local, final String fieldName, final int value) {
    generator.loadLocal(local);
    generator.push(value);
    generator.putField(myGridBagConstraintsType, fieldName, Type.INT_TYPE);
  }

  private void setDoubleField(final GeneratorAdapter generator, final int local, final String fieldName, final double value) {
    generator.loadLocal(local);
    generator.push(value);
    generator.putField(myGridBagConstraintsType, fieldName, Type.DOUBLE_TYPE);
  }
}
