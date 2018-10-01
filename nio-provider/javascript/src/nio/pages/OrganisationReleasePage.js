import React, {Component} from 'react';
import PropTypes from "prop-types";
import * as organisationService from "../services/OrganisationService";
import {OrganisationPage} from "./OrganisationPage";
import ReactTable from 'react-table';
import 'react-table/react-table.css';


export class OrganisationReleasePage extends Component {

  state = {
    filterNum: '',
    filterLabel: '',
    releases: [],
    loading: true
  };

  columns = [
    {
      title: 'Version',
      content: item => item.version.num,
      filteredState: 'filterNum',
    }, {
      title: 'Libellé',
      content: item => item.label,
      filteredState: 'filterLabel'
    }, {
      title: 'Dernière modif.',
      content: item => item.version.lastUpdate,
      notFilterable: true
    }, {
      title: 'Afficher les détails',
      content: item => item.version,
      notFilterable: true,
      cell: (v, item) => <i style={{cursor: 'pointer'}} className="glyphicon glyphicon-eye-open consultColor"
                            onClick={() => this.props.showDetails(item.version.num)}/>
    }
  ];


  componentDidMount() {

    this.setState({loading: true}, () => {
      organisationService.getOrganisationReleasesHistoric(this.props.tenant, this.props.organisationKey)
        .then(releases => {
          this.setState({releases, loading: false});
        });
    });
  }

  onChange = (value, name) => {
    this.setState({[name]: value});
  };

  render() {

    if (this.state.loading)
      return "Loading...";


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
            <ReactTable
              className="fulltable -striped -highlight publications"
              data={this.state.releases}
              filterable={true}
              filterAll={true}
              defaultSorted={[{id: this.columns[0].title, desc: false}]}
              defaultFiltered={
                [
                  {id: this.columns[0].title, value: this.state.filterNum},
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
    )
  }
}

OrganisationReleasePage.propTypes = {
  tenant: PropTypes.string.isRequired,
  organisationKey: PropTypes.string.isRequired,
  showDetails: PropTypes.func.isRequired
};
