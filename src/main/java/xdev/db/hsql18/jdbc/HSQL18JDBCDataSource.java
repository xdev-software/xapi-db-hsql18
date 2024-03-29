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

import xdev.db.DBException;
import xdev.db.jdbc.JDBCDataSource;


public class HSQL18JDBCDataSource extends JDBCDataSource<HSQL18JDBCDataSource, HSQL18Dbms>
{
	public HSQL18JDBCDataSource()
	{
		super(new HSQL18Dbms());
	}
	
	@Override
	public Parameter[] getDefaultParameters()
	{
		return new Parameter[]{
			HOST.clone(), PORT.clone(9001), USERNAME.clone("sa"), PASSWORD.clone(),
			CATALOG.clone(), URL_EXTENSION.clone(), IS_SERVER_DATASOURCE.clone(),
			SERVER_URL.clone(), AUTH_KEY.clone()};
	}
	
	@Override
	protected HSQL18ConnectionInformation getConnectionInformation()
	{
		return new HSQL18ConnectionInformation(this.getHost(), this.getPort(), this.getUserName(), this.getPassword()
			.getPlainText(), this.getCatalog(), this.getUrlExtension(), this.getDbmsAdaptor());
	}
	
	@Override
	public HSQL18JDBCConnection openConnectionImpl() throws DBException
	{
		return new HSQL18JDBCConnection(this);
	}
	
	@Override
	public HSQL18JDBCMetaData getMetaData() throws DBException
	{
		return new HSQL18JDBCMetaData(this);
	}
	
	@Override
	public boolean canExport()
	{
		return false;
	}
}
