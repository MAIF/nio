import React, {Component} from 'react';
import PropTypes from "prop-types";
import * as consentService from "../services/ConsentService";
import {Link} from "react-router-dom";
import Moment from 'react-moment';
import ReactTable from 'react-table';
import 'react-table/react-table.css';
import {ConsentsPage} from "./Consents";
import {ConsentFactDisplayPage} from "./ConsentFactDisplayPage";

export class ConsentFactHistoryPage extends Component {

  state = {
    items: [],
    page: 0,
    pageSize: 20,
    count: 0,
    filterUserId: '',
    loading: true
  };

  columns = [
    {
      title: 'Date de mise à jour',
      content: item => item.lastUpdate,
      cell: (v, item) => {
        return item.lastUpdate ? <Moment locale="fr" parse="YYYY-MM-DDTHH:mm:ssZ"
                                         format="DD/MM/YYYY HH:mm:ss">{item.lastUpdate}</Moment> : "NC"
      },
      notFilterable: true
    }, {
      title: 'Version des consentements',
      content: item => item.version,
      notFilterable: true
    }, {
      title: 'Afficher les détails',
      content: item => item.version,
      notFilterable: true,
      cell: (v, item) => <i className="glyphicon glyphicon-eye-open"
                            onClick={() => this.props.showDetails(item)}/>
    }
  ];

  componentDidMount() {
    this.fetchData(this.state);
  }

  fetchData = (state, instance) => {
    consentService.getConsentsHistory(this.props.tenant, this.props.organisationKey, this.props.userId, state.page, state.pageSize)
      .then(consentsFactHistory => this.setState({
        items: consentsFactHistory.items,
        count: consentsFactHistory.count,
        page: consentsFactHistory.page,
        pageSize: consentsFactHistory.pageSize,
        loading: false
      }));
  };

  onChange = (search, name) => {
    this.setState({[name]: search});
  };

  render() {
    if (this.state.loading)
      return "Loading ...";


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
          <h3>Historique des modifications</h3>
        </div>
        <div className="col-md-12">
          <ReactTable
            className="fulltable -striped -highlight"
            manual // Forces table not to paginate or sort automatically so we can handle it server-side
            data={this.state.items}
            pages={Math.ceil(this.state.count / this.state.pageSize)} //Display to toal number of pages
            loading={this.state.loading}
            onFetchData={(state, instance) => {
              this.fetchData(state, instance)
            }}
            filterable
            defaultPageSize={20}
            pageSizeOptions={[5, 10, 20, 25, 50, 100]}
            columns={columns}
          />
        </div>
      </div>
    )
  }

}

ConsentFactHistoryPage.propTypes = {
  tenant: PropTypes.string.isRequired,
  organisationKey: PropTypes.string.isRequired,
  userId: PropTypes.string.isRequired,
  showDetails: PropTypes.func.isRequired
};

