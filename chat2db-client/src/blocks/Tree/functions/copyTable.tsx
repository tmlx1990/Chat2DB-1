// 置顶表格
import React from 'react';
import mysqlService from '@/service/sql';
import {Button, Form, Input} from 'antd';
import { openModal } from '@/store/common/components';
import styles from './copyTable.less';
import i18n from '@/i18n';

export const copyTable = (treeNodeData,loadData) => {
  openModal({
    width: '450px',
    content: <CopyTableModalContent treeNodeData={treeNodeData} loadData={loadData} openModal={openModal} />,
  });
};

export const CopyTableModalContent = (params: { treeNodeData: any; openModal: any; loadData: any }) => {
  const { treeNodeData,loadData } = params;

  const onOk = () => {
    const p: any = {
      dataSourceId: treeNodeData.extraParams.dataSourceId,
      databaseName: treeNodeData.extraParams.databaseName,
      schemaName: treeNodeData.extraParams.schemaName,
      tableName: treeNodeData.name,
    };
    mysqlService.deleteTable(p).then(() => {
      loadData({
        refresh: true,
        treeNodeData: treeNodeData.parentNode
      });
      openModal(false);
    });
  };

  return (
    <div className={styles.copyModalContent}>
      <div className={styles.tableTop}>
        <Form.Item label={`${i18n('editTable.label.tableName')}:`} name="newTableName">
          <Input />
        </Form.Item>
      </div>
      <div className={styles.copyTableFooter}>
        <Button
          type="primary"
          onClick={() => {
            openModal(false);
          }}
        >
          {i18n('common.button.cancel')}
        </Button>
        <Button onClick={onOk}>
          {i18n('common.button.affirm')}
        </Button>
      </div>
    </div>
  );
};
