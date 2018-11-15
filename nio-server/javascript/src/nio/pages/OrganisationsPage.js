import React, {Component} from 'react';
import PropTypes from "prop-types";
import * as organisationService from "../services/OrganisationService";
import {Link} from 'react-router-dom';
import ReactTable from 'react-table';
import 'react-table/react-table.css';

export class OrganisationsPage extends Component {

  columns = [
    {
      title: 'Clé',
      content: item => item.key,
      cell: (v, item) => {
        return <Link to={`/organisations/${item.key}`} style={{cursor: 'pointer'}}>{item.key} <i
          className="glyphicon glyphicon-share"/></Link>
      },
      filteredState: 'filterKey',
    }, {
      title: 'Libellé',
      filteredState: 'filterLabel',
      content: item => item.label
    }, {
      title: 'Version',
      content: item => item.version.num,
      notFilterable: true,
      cell: (v, item) => {
        if (item.version.status === "RELEASED")
          return <span>{item.version.num}</span>;

        return <span>{item.version.num} (brouillon)</span>;
      }
    }, {
      title: 'Version publiée',
      content: item => item.version.status,
      notFilterable: true,
      cell: (v, item) => {
        if (item.version.status === "RELEASED")
          return "Oui";

        return "Non";
      }
    }, {
      title: 'Utilisateurs',
      notFilterable: true,
      content: item => item.label,
      cell: (v, item) => {
        return <Link to={`/organisations/${item.key}/users`} style={{cursor: 'pointer'}}><i
          className="glyphicon glyphicon-eye-open consultColor"/></Link>
      }
    }
  ];

  state = {
    organisations: [],
    filterLabel: '',
    filterKey: '',
    loading: true
  };

  componentDidMount() {
    this.setState({loading: true}, () => {
      organisationService.getOrganisations(this.props.tenant)
        .then(organisations => {
          this.setState({organisations, loading: false});
        })
    });
  }

  onChange = (search, name) => {
    this.setState({[name]: search});
  };

  render() {
    if (this.state.loading) {
      return "Loading...";
    }

    const columns = this.columns.map(c => {
      return {
        Header: c.title,
        id: c.title,
        headerStyle: c.style,
        width: c.style && c.style.width ? c.style.width : undefined,
        style: {...c.style, height: 30},
        sortable: !c.notSortable,
        filterable: !c.notFilterable,
        accessor: d => (c.content ? c.content(d) : d),
        Filter: d => (
          <input
            type="text"
            className="form-control input-sm"
            value={c.filteredState ? this.state[c.filteredState] : ''}
            onChange={e => {
              this.onChange(e.target.value, c.filteredState)
            }}
            placeholder="Search ..."
          />
        ),
        Cell: r => {
          const value = r.value;
          const original = r.original;
          return c.cell ? (
            c.cell(value, original, this)
          ) : (
            value
          );
        },
      };
    });

    return (
      <div className="row">
        <div className="col-md-12">
          <h3>Organisations</h3>
        </div>
        <div className="col-md-12 clearfix" style={{marginBottom: 20}}>
          <Link className="btn btn-success pull-right" to="/organisations/new" style={{cursor: 'pointer'}}>Nouvelle
            organisation</Link>
        </div>
        <div className="row">
          <div className="col-md-12">
            <ReactTable
              className="fulltable -striped -highlight organisations"
              data={this.state.organisations}
              filterable={true}
              filterAll={true}
              defaultSorted={[{id: this.columns[0].title, desc: false}]}
              defaultFiltered={
                [
                  {id: this.columns[0].title, value: this.state.filterKey},
                  {id: this.columns[1].title, value: this.state.filterLabel}
                ]
              }
              defaultPageSize={20}
              columns={columns}
              defaultFilterMethod={(filter, row, column) => {
                const id = filter.pivotId || filter.id;
                if (row[id] !== undefined) {
                  const value = String(row[id]);
                  return value.toLowerCase().indexOf(filter.value.toLowerCase()) > -1;
                } else {
                  return true;
                }
              }}
            />
          </div>
        </div>
      </div>
    );
  }
}

OrganisationsPage.propTypes = {
  tenant: PropTypes.string.isRequired
};
