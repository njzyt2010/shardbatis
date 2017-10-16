/**
 * 
 */
package com.google.code.shardbatis.converter;

import java.util.Iterator;

import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnalyticExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.CastExpression;
import net.sf.jsqlparser.expression.DateTimeLiteralExpression;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.ExtractExpression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.HexValue;
import net.sf.jsqlparser.expression.IntervalExpression;
//import net.sf.jsqlparser.expression.InverseExpression;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.JsonExpression;
import net.sf.jsqlparser.expression.KeepExpression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.MySQLGroupConcat;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.NumericBind;
import net.sf.jsqlparser.expression.OracleHierarchicalExpression;
import net.sf.jsqlparser.expression.OracleHint;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.RowConstructor;
import net.sf.jsqlparser.expression.SignedExpression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeKeyExpression;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.UserVariable;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.WithinGroupExpression;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseAnd;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseOr;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseXor;
import net.sf.jsqlparser.expression.operators.arithmetic.Concat;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.Modulo;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsListVisitor;
import net.sf.jsqlparser.expression.operators.relational.JsonOperator;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.Matches;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MultiExpressionList;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.expression.operators.relational.RegExpMatchOperator;
import net.sf.jsqlparser.expression.operators.relational.RegExpMySQLOperator;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItemVisitor;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.LateralSubSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectVisitor;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.TableFunction;
//import net.sf.jsqlparser.statement.select.Union;
import net.sf.jsqlparser.statement.select.ValuesList;
import net.sf.jsqlparser.statement.select.WithItem;

/**
 * @author sean.he
 * 
 */
public class SelectSqlConverter extends AbstractSqlConverter {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.google.code.shardbatis.converter.AbstractSqlConverter#doConvert(net
	 * .sf.jsqlparser.statement.Statement, java.lang.Object, java.lang.String)
	 */
	protected Statement doConvert(Statement statement, final Object params,
			final String mapperId) {
		if (!(statement instanceof Select)) {
			throw new IllegalArgumentException(
					"The argument statement must is instance of Select.");
		}
		TableNameModifier modifier = new TableNameModifier(params, mapperId);
		((Select) statement).getSelectBody().accept(modifier);
		return statement;
	}
	
	private class TableNameModifier implements SelectVisitor
	, FromItemVisitor
	,ExpressionVisitor
	,ItemsListVisitor 
	{
		private Object params;
		private String mapperId;

		TableNameModifier(Object params, String mapperId) {
			this.params = params;
			this.mapperId = mapperId;
		}
//		##################SELECT
		@Override
		public void visit(PlainSelect plainSelect) {
			plainSelect.getFromItem().accept(this);

			if (plainSelect.getJoins() != null) {
				for (Iterator joinsIt = plainSelect.getJoins().iterator(); joinsIt.hasNext();) {
					Join join = (Join) joinsIt.next();
					join.getRightItem().accept(this);
				}
			}
			if (plainSelect.getWhere() != null)
				plainSelect.getWhere().accept(this);
		}

		@Override
		public void visit(SetOperationList setOpList) {
//			TODO 没有
		}

		@Override
		public void visit(WithItem withItem) {
//			TODO 没有
		}

//		################################FROM

		@Override
		public void visit(Table tableName) {
			String table = tableName.getName();
			table = convertTableName(table, params, mapperId);
			// convert table name
			tableName.setName(table);
		}

		@Override
		public void visit(SubSelect subSelect) {
			subSelect.getSelectBody().accept(this);
		}

		@Override
		public void visit(SubJoin subjoin) {
			subjoin.getLeft().accept(this);
			subjoin.getJoin().getRightItem().accept(this);
		}

		@Override
		public void visit(LateralSubSelect lateralSubSelect) {
//			TODO 没有
		}

		@Override
		public void visit(ValuesList valuesList) {
//			TODO 没有			
		}

		@Override
		public void visit(TableFunction tableFunction) {
//			TODO 没有		
		}
//		#####################
		@Override
		public void visit(NullValue nullValue) {
//			不用实现		
		}
		@Override
		public void visit(Function function) {
//			不用实现		
		}
		@Override
		public void visit(SignedExpression signedExpression) {
//			TODO 没有	
		}
		@Override
		public void visit(JdbcParameter jdbcParameter) {
//		不用实现			
		}
		@Override
		public void visit(JdbcNamedParameter jdbcNamedParameter) {
//			TODO 没有	
		}
		@Override
		public void visit(DoubleValue doubleValue) {
//			不用实现				
		}
		@Override
		public void visit(LongValue longValue) {
//			不用实现				
		}
		@Override
		public void visit(HexValue hexValue) {
//			TODO 没有
		}
		@Override
		public void visit(DateValue dateValue) {
//			不用实现					
		}
		@Override
		public void visit(TimeValue timeValue) {
//			不用实现					
		}
		@Override
		public void visit(TimestampValue timestampValue) {
//			不用实现					
		}
		@Override
		public void visit(Parenthesis parenthesis) {
			parenthesis.getExpression().accept(this);			
		}
		@Override
		public void visit(StringValue stringValue) {
//			不用实现					
		}
		public void visitBinaryExpression(BinaryExpression binaryExpression) {
			binaryExpression.getLeftExpression().accept(this);
			binaryExpression.getRightExpression().accept(this);
		}
		@Override
		public void visit(Addition addition) {
			visitBinaryExpression(addition);			
		}
		@Override
		public void visit(Division division) {
			visitBinaryExpression(division);			
		}
		@Override
		public void visit(Multiplication multiplication) {
			visitBinaryExpression(multiplication);			
		}
		@Override
		public void visit(Subtraction subtraction) {
			visitBinaryExpression(subtraction);			
		}
		@Override
		public void visit(AndExpression andExpression) {
			visitBinaryExpression(andExpression);			
		}
		@Override
		public void visit(OrExpression orExpression) {
			visitBinaryExpression(orExpression);			
		}
		@Override
		public void visit(Between between) {
			between.getLeftExpression().accept(this);
			between.getBetweenExpressionStart().accept(this);
			between.getBetweenExpressionEnd().accept(this);			
		}
		@Override
		public void visit(EqualsTo equalsTo) {
			visitBinaryExpression(equalsTo);			
		}
		@Override
		public void visit(GreaterThan greaterThan) {
			visitBinaryExpression(greaterThan);			
		}
		@Override
		public void visit(GreaterThanEquals greaterThanEquals) {
			visitBinaryExpression(greaterThanEquals);			
		}
		@Override
		public void visit(InExpression inExpression) {
			inExpression.getLeftExpression().accept(this);
//			inExpression.getItemsList().accept(this);	
			inExpression.getRightItemsList().accept(this);
		}
		@Override
		public void visit(IsNullExpression isNullExpression) {
//			不用实现					
		}
		@Override
		public void visit(LikeExpression likeExpression) {
			visitBinaryExpression(likeExpression);			
		}
		@Override
		public void visit(MinorThan minorThan) {
			visitBinaryExpression(minorThan);			
		}
		@Override
		public void visit(MinorThanEquals minorThanEquals) {
			visitBinaryExpression(minorThanEquals);			
		}
		@Override
		public void visit(NotEqualsTo notEqualsTo) {
			visitBinaryExpression(notEqualsTo);			
		}
		@Override
		public void visit(Column tableColumn) {
//			不用实现				
		}
		@Override
		public void visit(CaseExpression caseExpression) {
//			不用实现					
		}
		@Override
		public void visit(WhenClause whenClause) {
//			不用实现					
		}
		@Override
		public void visit(ExistsExpression existsExpression) {
			existsExpression.getRightExpression().accept(this);			
		}
		@Override
		public void visit(AllComparisonExpression allComparisonExpression) {
			allComparisonExpression.getSubSelect().getSelectBody().accept(this);			
		}
		@Override
		public void visit(AnyComparisonExpression anyComparisonExpression) {
			anyComparisonExpression.getSubSelect().getSelectBody().accept(this);			
		}
		@Override
		public void visit(Concat concat) {
			visitBinaryExpression(concat);			
		}
		@Override
		public void visit(Matches matches) {
			visitBinaryExpression(matches);			
		}
		@Override
		public void visit(BitwiseAnd bitwiseAnd) {
			visitBinaryExpression(bitwiseAnd);			
		}
		@Override
		public void visit(BitwiseOr bitwiseOr) {
			visitBinaryExpression(bitwiseOr);			
		}
		@Override
		public void visit(BitwiseXor bitwiseXor) {
			visitBinaryExpression(bitwiseXor);			
		}
		@Override
		public void visit(CastExpression cast) {
//			TODO 没有			
		}
		@Override
		public void visit(Modulo modulo) {
//			TODO 没有
		}
		@Override
		public void visit(AnalyticExpression aexpr) {
//			TODO 没有			
		}
		@Override
		public void visit(WithinGroupExpression wgexpr) {
//			TODO 没有			
		}
		@Override
		public void visit(ExtractExpression eexpr) {
//			TODO 没有			
		}
		@Override
		public void visit(IntervalExpression iexpr) {
//			TODO 没有			
		}
		@Override
		public void visit(OracleHierarchicalExpression oexpr) {
//			TODO 没有			
		}
		@Override
		public void visit(RegExpMatchOperator rexpr) {
//			TODO 没有			
		}
		@Override
		public void visit(JsonExpression jsonExpr) {
//			TODO 没有			
		}
		@Override
		public void visit(JsonOperator jsonExpr) {
//			TODO 没有			
		}
		@Override
		public void visit(RegExpMySQLOperator regExpMySQLOperator) {
//			TODO 没有			
		}
		@Override
		public void visit(UserVariable var) {
//			TODO 没有			
		}
		@Override
		public void visit(NumericBind bind) {
//			TODO 没有			
		}
		@Override
		public void visit(KeepExpression aexpr) {
//			TODO 没有			
		}
		@Override
		public void visit(MySQLGroupConcat groupConcat) {
//			TODO 没有			
		}
		@Override
		public void visit(RowConstructor rowConstructor) {
//			TODO 没有			
		}
		@Override
		public void visit(OracleHint hint) {
//			TODO 没有			
		}
		@Override
		public void visit(TimeKeyExpression timeKeyExpression) {
//			TODO 没有			
		}
		@Override
		public void visit(DateTimeLiteralExpression literal) {
//			TODO 没有			
		}
		@Override
		public void visit(NotExpression aThis) {
//			TODO 没有			
		}
		@Override
		public void visit(ExpressionList expressionList) {
			for (Iterator iter = expressionList.getExpressions().iterator(); iter
			.hasNext();) {
				Expression expression = (Expression) iter.next();
				expression.accept(this);
			}			
		}
		@Override
		public void visit(MultiExpressionList multiExprList) {
//			TODO 没有	
		}

	}

//	private class TableNameModifier implements SelectVisitor, FromItemVisitor,
//			ExpressionVisitor, ItemsListVisitor {
//		private Object params;
//		private String mapperId;
//
//		TableNameModifier(Object params, String mapperId) {
//			this.params = params;
//			this.mapperId = mapperId;
//		}
//
//		@SuppressWarnings("unchecked")
//		public void visit(PlainSelect plainSelect) {
//			plainSelect.getFromItem().accept(this);
//
//			if (plainSelect.getJoins() != null) {
//				for (Iterator joinsIt = plainSelect.getJoins().iterator(); joinsIt
//						.hasNext();) {
//					Join join = (Join) joinsIt.next();
//					join.getRightItem().accept(this);
//				}
//			}
//			if (plainSelect.getWhere() != null)
//				plainSelect.getWhere().accept(this);
//
//		}
//
//		@SuppressWarnings("unchecked")
//		public void visit(Union union) {
//			for (Iterator iter = union.getPlainSelects().iterator(); iter
//					.hasNext();) {
//				PlainSelect plainSelect = (PlainSelect) iter.next();
//				visit(plainSelect);
//			}
//		}
//
//		public void visit(Table tableName) {
//			String table = tableName.getName();
//			table = convertTableName(table, params, mapperId);
//			// convert table name
//			tableName.setName(table);
//		}
//
//		public void visit(SubSelect subSelect) {
//			subSelect.getSelectBody().accept(this);
//		}
//
//		public void visit(Addition addition) {
//			visitBinaryExpression(addition);
//		}
//
//		public void visit(AndExpression andExpression) {
//			visitBinaryExpression(andExpression);
//		}
//
//		public void visit(Between between) {
//			between.getLeftExpression().accept(this);
//			between.getBetweenExpressionStart().accept(this);
//			between.getBetweenExpressionEnd().accept(this);
//		}
//
//		public void visit(Column tableColumn) {
//		}
//
//		public void visit(Division division) {
//			visitBinaryExpression(division);
//		}
//
//		public void visit(DoubleValue doubleValue) {
//		}
//
//		public void visit(EqualsTo equalsTo) {
//			visitBinaryExpression(equalsTo);
//		}
//
//		public void visit(Function function) {
//		}
//
//		public void visit(GreaterThan greaterThan) {
//			visitBinaryExpression(greaterThan);
//		}
//
//		public void visit(GreaterThanEquals greaterThanEquals) {
//			visitBinaryExpression(greaterThanEquals);
//		}
//
//		public void visit(InExpression inExpression) {
//			inExpression.getLeftExpression().accept(this);
//			inExpression.getItemsList().accept(this);
//		}
//
//		public void visit(InverseExpression inverseExpression) {
//			inverseExpression.getExpression().accept(this);
//		}
//
//		public void visit(IsNullExpression isNullExpression) {
//		}
//
//		public void visit(JdbcParameter jdbcParameter) {
//		}
//
//		public void visit(LikeExpression likeExpression) {
//			visitBinaryExpression(likeExpression);
//		}
//
//		public void visit(ExistsExpression existsExpression) {
//			existsExpression.getRightExpression().accept(this);
//		}
//
//		public void visit(LongValue longValue) {
//		}
//
//		public void visit(MinorThan minorThan) {
//			visitBinaryExpression(minorThan);
//		}
//
//		public void visit(MinorThanEquals minorThanEquals) {
//			visitBinaryExpression(minorThanEquals);
//		}
//
//		public void visit(Multiplication multiplication) {
//			visitBinaryExpression(multiplication);
//		}
//
//		public void visit(NotEqualsTo notEqualsTo) {
//			visitBinaryExpression(notEqualsTo);
//		}
//
//		public void visit(NullValue nullValue) {
//		}
//
//		public void visit(OrExpression orExpression) {
//			visitBinaryExpression(orExpression);
//		}
//
//		public void visit(Parenthesis parenthesis) {
//			parenthesis.getExpression().accept(this);
//		}
//
//		public void visit(Subtraction subtraction) {
//			visitBinaryExpression(subtraction);
//		}
//
//		public void visitBinaryExpression(BinaryExpression binaryExpression) {
//			binaryExpression.getLeftExpression().accept(this);
//			binaryExpression.getRightExpression().accept(this);
//		}
//
//		@SuppressWarnings("unchecked")
//		public void visit(ExpressionList expressionList) {
//			for (Iterator iter = expressionList.getExpressions().iterator(); iter
//					.hasNext();) {
//				Expression expression = (Expression) iter.next();
//				expression.accept(this);
//			}
//
//		}
//
//		public void visit(DateValue dateValue) {
//		}
//
//		public void visit(TimestampValue timestampValue) {
//		}
//
//		public void visit(TimeValue timeValue) {
//		}
//
//		/*
//		 * (non-Javadoc)
//		 * 
//		 * @see
//		 * net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser
//		 * .expression.CaseExpression)
//		 */
//		public void visit(CaseExpression caseExpression) {
//			// TODO Auto-generated method stub
//
//		}
//
//		/*
//		 * (non-Javadoc)
//		 * 
//		 * @see
//		 * net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser
//		 * .expression.WhenClause)
//		 */
//		public void visit(WhenClause whenClause) {
//			// TODO Auto-generated method stub
//
//		}
//
//		public void visit(AllComparisonExpression allComparisonExpression) {
//			allComparisonExpression.GetSubSelect().getSelectBody().accept(this);
//		}
//
//		public void visit(AnyComparisonExpression anyComparisonExpression) {
//			anyComparisonExpression.GetSubSelect().getSelectBody().accept(this);
//		}
//
//		public void visit(SubJoin subjoin) {
//			subjoin.getLeft().accept(this);
//			subjoin.getJoin().getRightItem().accept(this);
//		}
//
//		public void visit(Concat concat) {
//			visitBinaryExpression(concat);
//		}
//
//		public void visit(Matches matches) {
//			visitBinaryExpression(matches);
//
//		}
//
//		public void visit(BitwiseAnd bitwiseAnd) {
//			visitBinaryExpression(bitwiseAnd);
//
//		}
//
//		public void visit(BitwiseOr bitwiseOr) {
//			visitBinaryExpression(bitwiseOr);
//
//		}
//
//		public void visit(BitwiseXor bitwiseXor) {
//			visitBinaryExpression(bitwiseXor);
//		}
//
//		public void visit(StringValue stringValue) {
//		}
//	}

}
