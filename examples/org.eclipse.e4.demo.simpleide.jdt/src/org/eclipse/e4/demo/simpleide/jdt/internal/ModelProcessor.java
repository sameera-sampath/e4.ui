/*******************************************************************************
 * Copyright (c) 2010 BestSolution.at and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tom Schindl <tom.schindl@bestsolution.at> - initial API and implementation
 ******************************************************************************/
package org.eclipse.e4.demo.simpleide.jdt.internal;

import org.eclipse.e4.demo.simpleide.model.simpleide.MEditorPartDescriptor;
import org.eclipse.e4.demo.simpleide.model.simpleide.MSimpleIDEApplication;
import org.eclipse.e4.demo.simpleide.model.simpleide.MSimpleideFactory;
import org.eclipse.e4.workbench.modeling.IModelExtension;
import org.eclipse.emf.ecore.EObject;

public class ModelProcessor implements IModelExtension {

	public void processElement(EObject parent) {
		if( parent instanceof MSimpleIDEApplication ) {
			MSimpleIDEApplication app = (MSimpleIDEApplication) parent;
			
			MEditorPartDescriptor desc = MSimpleideFactory.INSTANCE.createEditorPartDescriptor();
			desc.setContributionURI("platform:/plugin/org.eclipse.e4.demo.simpleide.jdt/org.eclipse.e4.demo.simpleide.jdt.internal.editor.JavaEditor");
			desc.setIconURI("platform:/plugin/org.eclipse.e4.demo.simpleide.jdt/icons/jcu_obj.gif");
			desc.getFileextensions().add("java");
			app.getEditorPartDescriptors().add(desc);
			System.err.println("Adding Java Editor");
		}
	}
}