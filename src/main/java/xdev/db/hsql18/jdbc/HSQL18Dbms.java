/*
 * SqlEngine Database Adapter HSQL18 - XAPI SqlEngine Database Adapter for HSQL18
 * Copyright © 2003 XDEV Software (https://xdev.software)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package xdev.db.hsql18.jdbc;

import com.xdev.jadoth.sqlengine.dbms.DbmsAdaptor;
import com.xdev.jadoth.sqlengine.dbms.SQLExceptionParser;
import com.xdev.jadoth.sqlengine.internal.DatabaseGateway;
import com.xdev.jadoth.sqlengine.internal.tables.SqlTableIdentity;


public class HSQL18Dbms
	extends
	DbmsAdaptor.Implementation<HSQL18Dbms, HSQL18DMLAssembler, HSQL18DDLMapper, HSQL18RetrospectionAccessor,
		HSQL18Syntax>
{
	// /////////////////////////////////////////////////////////////////////////
	// constants //
	// ///////////////////
	
	public static final HSQL18Syntax SYNTAX = new HSQL18Syntax();
	/**
	 * The Constant MAX_VARCHAR_LENGTH.
	 */
	protected static final int MAX_VARCHAR_LENGTH = Integer.MAX_VALUE;
	protected static final char IDENTIFIER_DELIMITER = '"';
	
	// /////////////////////////////////////////////////////////////////////////
	// constructors //
	// ///////////////////
	
	public HSQL18Dbms()
	{
		this(new SQLExceptionParser.Body());
	}
	
	/**
	 * @param sqlExceptionParser the sql exception parser
	 */
	public HSQL18Dbms(final SQLExceptionParser sqlExceptionParser)
	{
		super(sqlExceptionParser, false);
		this.setRetrospectionAccessor(new HSQL18RetrospectionAccessor(this));
		this.setDMLAssembler(new HSQL18DMLAssembler(this));
		this.setSyntax(SYNTAX);
	}
	
	/**
	 * @see DbmsAdaptor#createConnectionInformation(String, int, String, String, String, String)
	 */
	@Override
	public HSQL18ConnectionInformation createConnectionInformation(
		final String host,
		final int port, final String user, final String password, final String catalog, final String properties)
	{
		return new HSQL18ConnectionInformation(host, port, user, password, catalog, properties, this);
	}
	
	/**
	 * HSQL does not support any means of calculating table columns selectivity as far as it is known.
	 */
	@Override
	public Object updateSelectivity(final SqlTableIdentity table)
	{
		return null;
	}
	
	/**
	 * @see DbmsAdaptor#assembleTransformBytes(byte[], StringBuilder)
	 */
	@Override
	public StringBuilder assembleTransformBytes(final byte[] bytes, final StringBuilder sb)
	{
		return null;
	}
	
	/**
	 * @see DbmsAdaptor.Implementation#getRetrospectionAccessor()
	 */
	@Override
	public HSQL18RetrospectionAccessor getRetrospectionAccessor()
	{
		throw new RuntimeException("HSQL Retrospection not implemented yet!");
	}
	
	/**
	 * @see DbmsAdaptor#initialize(DatabaseGateway)
	 */
	@Override
	public void initialize(final DatabaseGateway<HSQL18Dbms> dbc)
	{
	}
	
	/**
	 * @see DbmsAdaptor#rebuildAllIndices(String)
	 */
	@Override
	public Object rebuildAllIndices(final String fullQualifiedTableName)
	{
		return null;
	}
	
	@Override
	public boolean supportsOFFSET_ROWS()
	{
		return true;
	}
	
	/**
	 * @see DbmsAdaptor#getMaxVARCHARlength()
	 */
	@Override
	public int getMaxVARCHARlength()
	{
		return MAX_VARCHAR_LENGTH;
	}
	
	@Override
	public char getIdentifierDelimiter()
	{
		return IDENTIFIER_DELIMITER;
	}
}
