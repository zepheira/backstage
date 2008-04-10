/**
 * 
 */
package edu.mit.simile.backstage.model.ui.lens;

import java.util.List;

import org.apache.log4j.Logger;
import org.mozilla.javascript.Scriptable;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.sail.SailConnection;

import edu.mit.simile.backstage.model.data.Database;
import edu.mit.simile.backstage.model.data.Expression;
import edu.mit.simile.backstage.model.data.ExpressionQueryResult;
import edu.mit.simile.backstage.model.ui.lens.SubcontentAttribute.ExpressionFragment;
import edu.mit.simile.backstage.model.ui.lens.SubcontentAttribute.Fragment;
import edu.mit.simile.backstage.model.ui.lens.SubcontentAttribute.StringFragment;
import edu.mit.simile.backstage.util.DefaultScriptableObject;
import edu.mit.simile.backstage.util.SailUtilities;
import edu.mit.simile.backstage.util.ScriptableArrayBuilder;

class DomLensNode extends LensNode {
    protected static Logger _logger = Logger.getLogger(DomLensNode.class);
    
	protected String		_control;
	
	protected String		_conditionTest;
	protected Expression	_conditionExpression;
	
	protected Expression				_contentExpression;
	protected List<ContentAttribute>	_contentAttributes;
	protected List<SubcontentAttribute>	_subcontentAttributes;
	
	protected List<LensNode>			_children;
	
	public DomLensNode() {
		
	}
	
	public void render(Value value, Scriptable result, Database database, SailRepositoryConnection connection) {
		if (!testCondition(value, result, database, connection)) {
			return;
		}
		
		if (_contentAttributes != null) {
			generateContentAttributes(value, result, database, connection);
		}
		if (_subcontentAttributes != null) {
			generateSubcontentAttributes(value, result, database, connection);
		}
		
		if (_contentExpression != null) {
			if (_children != null) {
				generateContentWithInnerTemplates(value, result, database, connection);
			} else {
				generateContent(value, result, database, connection);
			}
		} else if (_children != null) {
			ScriptableArrayBuilder arrayBuilder = new ScriptableArrayBuilder();
			
			for (LensNode node : _children) {
				if (node instanceof StringLensNode) {
					arrayBuilder.add("");
				} else {
			        DefaultScriptableObject o = new DefaultScriptableObject();
			        
			        ((DomLensNode) node).render(value, o, database, connection);
			        
					arrayBuilder.add(o);
				}
			}
			
			result.put("children", result, arrayBuilder.toArray());
		}
	}
	
	protected boolean testCondition(Value value, Scriptable result, Database database, SailRepositoryConnection connection) {
		if (_conditionTest == null) {
			return true;
		}
		
		boolean r = false;
		
        try {
			ExpressionQueryResult eqr = _conditionExpression.computeOutputOnValue(value, database, connection);
	        if (eqr != null) {
                TupleQueryResult queryResult = eqr.tupleQuery.evaluate();
                try {
					if ("if-exists".equals(_conditionTest)) {
						r = queryResult.hasNext();
					} else if ("if".equals(_conditionTest)) {
						if (queryResult.hasNext()) {
							BindingSet bindingSet = queryResult.next();
							Value value2 = bindingSet.getValue(eqr.resultVar.getName());
							if (value2 instanceof Literal) {
								r = ((Literal) value2).booleanValue();
							}
						}
					}
                } finally {
                    queryResult.close();
                }
	        } else {
				if (value instanceof Literal) {
					r = ((Literal) value).booleanValue();
				}
	        }
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		result.put("condition", result, r);
		
		return r;
	}
	
	protected void generateContentWithInnerTemplates(Value value, Scriptable result, Database database, SailRepositoryConnection connection) {
		ScriptableArrayBuilder arrayBuilder = new ScriptableArrayBuilder();
		
        try {
			ExpressionQueryResult eqr = _contentExpression.computeOutputOnValue(value, database, connection);
	        if (eqr != null) {
                TupleQueryResult queryResult = eqr.tupleQuery.evaluate();
                try {
                	while (queryResult.hasNext()) {
                		BindingSet bindingSet = queryResult.next();
                		Value value2 = bindingSet.getValue(eqr.resultVar.getName());
                		
                		arrayBuilder.add(generateInnerContentWithInnerTemplates(value2, database, connection));
                	}
                } finally {
                    queryResult.close();
                }
	        } else {
        		arrayBuilder.add(generateInnerContentWithInnerTemplates(value, database, connection));
	        }
		} catch (Exception e) {
			_logger.error("", e);
		}
		
		result.put("content", result, arrayBuilder.toArray());
	}
	
	protected Object generateInnerContentWithInnerTemplates(Value value, Database database, SailRepositoryConnection connection) {
		ScriptableArrayBuilder arrayBuilder = new ScriptableArrayBuilder();
		for (LensNode node : _children) {
			if (node instanceof StringLensNode) {
				arrayBuilder.add("");
			} else {
		        DefaultScriptableObject o = new DefaultScriptableObject();
		        
		        ((DomLensNode) node).render(value, o, database, connection);
		        
				arrayBuilder.add(o);
			}
		}
		return arrayBuilder.toArray();
	}
	
	protected void generateContent(Value value, Scriptable result, Database database, SailRepositoryConnection connection) {
		ScriptableArrayBuilder arrayBuilder = new ScriptableArrayBuilder();
		String valueType = "text";
		
        try {
			ExpressionQueryResult eqr = _contentExpression.computeOutputOnValue(value, database, connection);
	        if (eqr != null) {
                TupleQueryResult queryResult = eqr.tupleQuery.evaluate();
                try {
                	while (queryResult.hasNext()) {
                		BindingSet bindingSet = queryResult.next();
                		Value value2 = bindingSet.getValue(eqr.resultVar.getName());
                		
               			arrayBuilder.add(renderInnerValue(value2, database, connection));
                	}
                } finally {
                    queryResult.close();
                }
	        }
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        DefaultScriptableObject o = new DefaultScriptableObject();
        o.put("valueType", o, valueType);
        o.put("values", o, arrayBuilder.toArray());
        
		result.put("content", result, o);
	}
	
	protected void generateContentAttributes(Value value, Scriptable result, Database database, SailRepositoryConnection connection) {
		ScriptableArrayBuilder arrayBuilder = new ScriptableArrayBuilder();
		
		for (ContentAttribute a : _contentAttributes) {
	        DefaultScriptableObject o = new DefaultScriptableObject();
	        o.put("name", o, a.name);
	        
	        try {
	        	boolean first = true;
	        	StringBuffer sb = new StringBuffer();
	        	
				ExpressionQueryResult eqr = a.expression.computeOutputOnValue(value, database, connection);
		        if (eqr != null) {
	                TupleQueryResult queryResult = eqr.tupleQuery.evaluate();
	                try {
	                	while (queryResult.hasNext()) {
	                		BindingSet bindingSet = queryResult.next();
	                		Value value2 = bindingSet.getValue(eqr.resultVar.getName());
	                		
	                		if (first) {
	                			first = false;
	                		} else {
	                			sb.append(";");
	                		}
	                		
                			sb.append(renderInnerValueToText(value2, database, connection));
	                	}
	                } finally {
	                    queryResult.close();
	                }
		        }
		        
		        o.put("value", o, sb.toString());
			} catch (Exception e) {
				_logger.error("", e);
			}
			
			arrayBuilder.add(o);
		}
		
		result.put("contentAttributes", result, arrayBuilder.toArray());
	}
	
	protected void generateSubcontentAttributes(Value value, Scriptable result, Database database, SailRepositoryConnection connection) {
		ScriptableArrayBuilder arrayBuilder = new ScriptableArrayBuilder();
		
		for (SubcontentAttribute a : _subcontentAttributes) {
	        DefaultScriptableObject o = new DefaultScriptableObject();
	        o.put("name", o, a.name);
	        
        	StringBuffer sb = new StringBuffer();
        	for (Fragment f : a.fragments) {
        		if (f instanceof StringFragment) {
        			sb.append(((StringFragment) f).text);
        		} else {
        	        try {
			        	boolean first = true;
						ExpressionQueryResult eqr = ((ExpressionFragment) f).expression.computeOutputOnValue(
								value, database, connection);
						
				        if (eqr != null) {
			                TupleQueryResult queryResult = eqr.tupleQuery.evaluate();
			                try {
			                	while (queryResult.hasNext()) {
			                		BindingSet bindingSet = queryResult.next();
			                		Value value2 = bindingSet.getValue(eqr.resultVar.getName());
			                		
			                		if (first) {
			                			first = false;
			                		} else {
			                			sb.append(";");
			                		}
			                		
		                			sb.append(renderInnerValueToText(value2, database, connection));
			                	}
			                } finally {
			                    queryResult.close();
			                }
				        } else {
				        	sb.append(renderInnerValueToText(value, database, connection));
				        }
					} catch (Exception e) {
						_logger.error(e);
					}
        		}
        	}
	        o.put("value", o, sb.toString());
	        
			arrayBuilder.add(o);
		}
		
		result.put("subcontentAttributes", result, arrayBuilder.toArray());
	}
	
	protected Object renderInnerValue(Value value, Database database, SailRepositoryConnection connection) {
		if (value instanceof URI) {
			return renderInnerItem((URI) value, database, connection);
		} else {
			return ((Literal) value).getLabel();
		}
	}
	
	protected Scriptable renderInnerItem(URI item, Database database, SailRepositoryConnection connection) {
		String id = database.getItemId(item);
		
        DefaultScriptableObject o = new DefaultScriptableObject();
        o.put("itemID", o, id);
        
		if (_children == null) {
			o.put("label", o, SailUtilities.getStringObject((SailConnection) connection, item, RDFS.LABEL, id));
		} else {
			// TODO
		}
		
		return o;
	}
	
	protected String renderInnerValueToText(Value value, Database database, SailRepositoryConnection connection) {
		if (value instanceof URI) {
			return renderInnerItemToText((URI) value, database, connection);
		} else {
			return ((Literal) value).getLabel();
		}
	}
	
	protected String renderInnerItemToText(URI item, Database database, SailRepositoryConnection connection) {
		return database.getItemId(item);
	}
}
