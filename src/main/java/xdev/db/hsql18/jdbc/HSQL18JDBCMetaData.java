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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import xdev.db.ColumnMetaData;
import xdev.db.DBException;
import xdev.db.DataType;
import xdev.db.Index;
import xdev.db.Index.IndexType;
import xdev.db.Result;
import xdev.db.jdbc.JDBCConnection;
import xdev.db.jdbc.JDBCMetaData;
import xdev.db.sql.Functions;
import xdev.db.sql.SELECT;
import xdev.db.sql.Table;
import xdev.util.ProgressMonitor;


public class HSQL18JDBCMetaData extends JDBCMetaData
{
	private static final long serialVersionUID = 6496434115983447535L;
	
	public HSQL18JDBCMetaData(final HSQL18JDBCDataSource dataSource) throws DBException
	{
		super(dataSource);
	}
	
	private static void addMetaDataToColumns(
		final TableInfo table,
		final Map<String, Boolean> autoIncrementMap,
		final Result result,
		final List<ColumnMetaData> columns) throws DBException
	{
		final String columnName = result.getString("COLUMN_NAME");
		Object defaultValue = result.getObject("COLUMN_DEF");
		if("NULL".equals(defaultValue))
		{
			defaultValue = null;
		}
		columns.add(new ColumnMetaData(
			table.getName(),                                        // tableName
			columnName,                                             // columnName
			"",                                                     // caption
			DataType.get(result.getInt("DATA_TYPE")),       // type
			result.getInt("COLUMN_SIZE"),                   // length
			result.getInt("DECIMAL_DIGITS"),                // scale
			defaultValue,                                           // defaultValue
			"YES".equals(result.getString("IS_NULLABLE")),  // nullable
			Boolean.TRUE.equals(autoIncrementMap.get(columnName))   // autoIncrement
		));
	}
	
	@Override
	public TableInfo[] getTableInfos(final ProgressMonitor monitor, final EnumSet<TableType> types)
		throws DBException
	{
		monitor.beginTask("", ProgressMonitor.UNKNOWN);
		
		final List<TableInfo> list = new ArrayList<>();
		
		try(final JDBCConnection jdbcConnection = (JDBCConnection)this.dataSource.openConnection())
		{
			final String tableTypeStatement = this.getTableTypeStatement(types);
			final Result result = jdbcConnection.query("SELECT TABLE_SCHEM, TABLE_NAME, TABLE_TYPE "
				+ "FROM INFORMATION_SCHEMA.SYSTEM_TABLES " + "WHERE TABLE_TYPE in "
				+ tableTypeStatement);
			
			while(result.next() && !monitor.isCanceled())
			{
				final String table_type = result.getString("TABLE_TYPE");
				
				TableType type = null;
				if(table_type.equals("TABLE"))
				{
					type = TableType.TABLE;
				}
				else if(table_type.equals("VIEW"))
				{
					type = TableType.VIEW;
				}
				
				if(type != null && types.contains(type))
				{
					list.add(new TableInfo(type, result.getString("TABLE_SCHEM"), result
						.getString("TABLE_NAME")));
				}
			}
			
			result.close();
		}
		
		monitor.done();
		
		final TableInfo[] tables = list.toArray(new TableInfo[list.size()]);
		Arrays.sort(tables);
		return tables;
	}
	
	@Override
	protected TableMetaData getTableMetaData(
		final JDBCConnection jdbcConnection, final DatabaseMetaData meta,
		final int flags, final TableInfo table) throws DBException, SQLException
	{
		final String tableName = table.getName();
		final Table tableIdentity = new Table(tableName);
		final Map<String, Boolean> autoIncrementMap = new HashMap<>();
		final SELECT select = new SELECT().FROM(tableIdentity).WHERE("1 = 0");
		Result result = jdbcConnection.query(select);
		final int cc = result.getColumnCount();
		
		for(int i = 0; i < cc; i++)
		{
			final ColumnMetaData cm = result.getMetadata(i);
			autoIncrementMap.put(cm.getName(), cm.isAutoIncrement());
		}
		result.close();
		
		result = jdbcConnection.query("SELECT * FROM INFORMATION_SCHEMA.SYSTEM_COLUMNS "
			+ "WHERE TABLE_NAME = ?", table.getName());
		
		final List<ColumnMetaData> columns = new ArrayList<>();
		while(result.next())
		{
			addMetaDataToColumns(table, autoIncrementMap, result, columns);
		}
		result.close();
		
		final Map<IndexInfo, Set<String>> indexMap = new HashMap<>();
		final Set<String> primaryKeyColumns = new HashSet<>();
		String primaryKeyName = "PRIMARY_KEY";
		
		result = jdbcConnection.query("SELECT * FROM INFORMATION_SCHEMA.SYSTEM_PRIMARYKEYS "
			+ "WHERE TABLE_NAME = ?", table.getName());
		
		while(result.next())
		{
			primaryKeyColumns.add(result.getString("COLUMN_NAME"));
			primaryKeyName = result.getString("PK_NAME");
		}
		result.close();
		
		if((flags & INDICES) != 0)
		{
			if(primaryKeyColumns.size() > 0)
			{
				indexMap.put(new IndexInfo(primaryKeyName, IndexType.PRIMARY_KEY), primaryKeyColumns);
			}
			
			result = jdbcConnection.query("SELECT * FROM INFORMATION_SCHEMA.SYSTEM_INDEXINFO "
				+ "WHERE TABLE_NAME = ?", table.getName());
			while(result.next())
			{
				final String indexName = result.getString("INDEX_NAME");
				final String columnName = result.getString("COLUMN_NAME");
				if(indexName != null && columnName != null
					&& !primaryKeyColumns.contains(columnName))
				{
					final boolean unique = !result.getBoolean("NON_UNIQUE");
					final IndexInfo info = new IndexInfo(indexName, unique ? IndexType.UNIQUE
						: IndexType.NORMAL);
					Set<String> columnNames = indexMap.get(info);
					if(columnNames == null)
					{
						columnNames = new HashSet();
						indexMap.put(info, columnNames);
					}
					columnNames.add(columnName);
				}
			}
			result.close();
		}
		
		final Index[] indices = new Index[indexMap.size()];
		int i = 0;
		for(final IndexInfo indexInfo : indexMap.keySet())
		{
			final Set<String> columnList = indexMap.get(indexInfo);
			final String[] indexColumns = columnList.toArray(new String[columnList.size()]);
			indices[i++] = new Index(indexInfo.name, indexInfo.type, indexColumns);
		}
		
		int count = UNKNOWN_ROW_COUNT;
		
		if((flags & ROW_COUNT) != 0)
		{
			try
			{
				result = jdbcConnection.query(new SELECT().columns(Functions.COUNT()).FROM(
					tableIdentity));
				if(result.next())
				{
					count = result.getInt(0);
				}
				result.close();
			}
			catch(final Exception e)
			{
				e.printStackTrace();
			}
		}
		
		return new TableMetaData(table, columns.toArray(new ColumnMetaData[columns.size()]), indices,
			count);
	}
	
	private String getTableTypeStatement(final EnumSet<TableType> types)
	{
		if(types == null || types.size() <= 0)
		{
			return "";
		}
		
		String tableStatement = "(";
		
		if(types.contains(TableType.TABLE))
		{
			tableStatement += "'TABLE'";
		}
		
		if(types.contains(TableType.TABLE) && types.contains(TableType.VIEW))
		{
			tableStatement += " , ";
		}
		
		if(types.contains(TableType.VIEW))
		{
			tableStatement += "'VIEW'";
		}
		
		tableStatement += ")";
		
		return tableStatement;
	}
	
	@Override
	protected void createTable(final JDBCConnection jdbcConnection, final TableMetaData table)
		throws DBException, SQLException
	{
	}
	
	@Override
	protected void addColumn(
		final JDBCConnection jdbcConnection, final TableMetaData table,
		final ColumnMetaData column, final ColumnMetaData columnBefore, final ColumnMetaData columnAfter)
		throws DBException, SQLException
	{
	}
	
	@Override
	protected void alterColumn(
		final JDBCConnection jdbcConnection, final TableMetaData table,
		final ColumnMetaData column, final ColumnMetaData existing) throws DBException, SQLException
	{
	}
	
	@Override
	public boolean equalsType(final ColumnMetaData clientColumn, final ColumnMetaData dbColumn)
	{
		return false;
	}
	
	@Override
	protected void dropColumn(
		final JDBCConnection jdbcConnection, final TableMetaData table,
		final ColumnMetaData column) throws DBException, SQLException
	{
	}
	
	@Override
	protected void createIndex(final JDBCConnection jdbcConnection, final TableMetaData table, final Index index)
		throws DBException, SQLException
	{
	}
	
	@Override
	protected void dropIndex(final JDBCConnection jdbcConnection, final TableMetaData table, final Index index)
		throws DBException, SQLException
	{
	}
}
